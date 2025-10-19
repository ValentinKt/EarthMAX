-- Migration: Create Row Level Security Policies
-- This migration creates RLS policies for all tables

-- Profiles policies
CREATE POLICY "Users can view their own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert their own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- Eco tips policies (public read, admin write)
CREATE POLICY "Anyone can view active eco tips" ON public.eco_tips
    FOR SELECT USING (is_active = true AND deleted_at IS NULL);

CREATE POLICY "Authenticated users can view all eco tips" ON public.eco_tips
    FOR SELECT USING (auth.role() = 'authenticated');

-- User stats policies
CREATE POLICY "Users can view their own stats" ON public.user_stats
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own stats" ON public.user_stats
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own stats" ON public.user_stats
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Achievements policies (public read)
CREATE POLICY "Anyone can view active achievements" ON public.achievements
    FOR SELECT USING (is_active = true);

-- User achievements policies
CREATE POLICY "Users can view their own achievements" ON public.user_achievements
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own achievements" ON public.user_achievements
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own achievements" ON public.user_achievements
    FOR INSERT WITH CHECK (auth.uid() = user_id);

-- User eco tip completions policies
CREATE POLICY "Users can view their own completions" ON public.user_eco_tip_completions
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own completions" ON public.user_eco_tip_completions
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own completions" ON public.user_eco_tip_completions
    FOR UPDATE USING (auth.uid() = user_id);

-- User goals policies
CREATE POLICY "Users can view their own goals" ON public.user_goals
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own goals" ON public.user_goals
    FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own goals" ON public.user_goals
    FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own goals" ON public.user_goals
    FOR DELETE USING (auth.uid() = user_id);

-- Recent activities policies
CREATE POLICY "Users can view their own activities" ON public.recent_activities
    FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own activities" ON public.recent_activities
    FOR INSERT WITH CHECK (auth.uid() = user_id);