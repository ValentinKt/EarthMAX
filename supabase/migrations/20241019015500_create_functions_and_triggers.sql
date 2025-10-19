-- Migration: Create Functions and Triggers
-- This migration creates utility functions and triggers

-- Function to handle updated_at timestamps
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to handle new user profile creation
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, display_name)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'display_name', NEW.email)
    );
    
    -- Create initial user stats
    INSERT INTO public.user_stats (user_id)
    VALUES (NEW.id);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to update user stats after eco tip completion
CREATE OR REPLACE FUNCTION public.update_user_stats_on_completion()
RETURNS TRIGGER AS $$
DECLARE
    tip_carbon DECIMAL(10,2);
    tip_cost DECIMAL(10,2);
    current_date DATE := CURRENT_DATE;
    last_activity DATE;
    current_streak_val INTEGER;
BEGIN
    -- Get eco tip savings
    SELECT carbon_savings_kg, cost_savings_usd 
    INTO tip_carbon, tip_cost
    FROM public.eco_tips 
    WHERE id = NEW.eco_tip_id;
    
    -- Get current user stats
    SELECT last_activity_date, current_streak
    INTO last_activity, current_streak_val
    FROM public.user_stats
    WHERE user_id = NEW.user_id;
    
    -- Calculate new streak
    IF last_activity IS NULL OR last_activity < current_date - INTERVAL '1 day' THEN
        -- Reset streak if more than 1 day gap
        IF last_activity = current_date - INTERVAL '1 day' THEN
            current_streak_val := current_streak_val + 1;
        ELSE
            current_streak_val := 1;
        END IF;
    END IF;
    
    -- Update user stats
    UPDATE public.user_stats
    SET 
        completed_tips_count = completed_tips_count + 1,
        total_carbon_saved_kg = total_carbon_saved_kg + COALESCE(NEW.carbon_saved_kg, tip_carbon, 0),
        total_cost_saved_usd = total_cost_saved_usd + COALESCE(NEW.cost_saved_usd, tip_cost, 0),
        current_streak = current_streak_val,
        longest_streak = GREATEST(longest_streak, current_streak_val),
        last_activity_date = current_date,
        updated_at = NOW()
    WHERE user_id = NEW.user_id;
    
    -- Add to recent activities
    INSERT INTO public.recent_activities (user_id, activity_type, activity_data)
    VALUES (
        NEW.user_id,
        'tip_completed',
        jsonb_build_object(
            'eco_tip_id', NEW.eco_tip_id,
            'completed_at', NEW.completed_at,
            'carbon_saved', COALESCE(NEW.carbon_saved_kg, tip_carbon, 0),
            'cost_saved', COALESCE(NEW.cost_saved_usd, tip_cost, 0)
        )
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to check and unlock achievements
CREATE OR REPLACE FUNCTION public.check_achievements()
RETURNS TRIGGER AS $$
DECLARE
    achievement_record RECORD;
    user_stats_record RECORD;
    progress_val INTEGER;
    should_unlock BOOLEAN;
BEGIN
    -- Get current user stats
    SELECT * INTO user_stats_record
    FROM public.user_stats
    WHERE user_id = NEW.user_id;
    
    -- Check all achievements for this user
    FOR achievement_record IN 
        SELECT a.* FROM public.achievements a
        LEFT JOIN public.user_achievements ua ON (ua.achievement_id = a.id AND ua.user_id = NEW.user_id)
        WHERE a.is_active = true AND (ua.is_completed IS NULL OR ua.is_completed = false)
    LOOP
        should_unlock := false;
        progress_val := 0;
        
        -- Check achievement requirements based on category
        CASE achievement_record.category
            WHEN 'MILESTONE' THEN
                -- Tips completed milestones
                IF achievement_record.requirements->>'type' = 'tips_completed' THEN
                    progress_val := user_stats_record.completed_tips_count;
                    should_unlock := progress_val >= (achievement_record.requirements->>'target')::INTEGER;
                END IF;
                
                -- Streak milestones
                IF achievement_record.requirements->>'type' = 'streak' THEN
                    progress_val := user_stats_record.current_streak;
                    should_unlock := progress_val >= (achievement_record.requirements->>'target')::INTEGER;
                END IF;
                
            WHEN 'ENVIRONMENTAL' THEN
                -- Carbon savings milestones
                IF achievement_record.requirements->>'type' = 'carbon_saved' THEN
                    progress_val := user_stats_record.total_carbon_saved_kg::INTEGER;
                    should_unlock := progress_val >= (achievement_record.requirements->>'target')::INTEGER;
                END IF;
        END CASE;
        
        -- Insert or update user achievement
        INSERT INTO public.user_achievements (user_id, achievement_id, progress, is_completed, completed_at)
        VALUES (
            NEW.user_id,
            achievement_record.id,
            progress_val,
            should_unlock,
            CASE WHEN should_unlock THEN NOW() ELSE NULL END
        )
        ON CONFLICT (user_id, achievement_id)
        DO UPDATE SET
            progress = EXCLUDED.progress,
            is_completed = EXCLUDED.is_completed,
            completed_at = EXCLUDED.completed_at,
            updated_at = NOW();
        
        -- Add to recent activities if unlocked
        IF should_unlock THEN
            INSERT INTO public.recent_activities (user_id, activity_type, activity_data)
            VALUES (
                NEW.user_id,
                'achievement_unlocked',
                jsonb_build_object(
                    'achievement_id', achievement_record.id,
                    'achievement_title', achievement_record.title,
                    'points', achievement_record.points
                )
            );
        END IF;
    END LOOP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;