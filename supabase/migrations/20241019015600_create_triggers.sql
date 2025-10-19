-- Migration: Create Triggers
-- This migration creates all necessary triggers

-- Trigger for updated_at on profiles
CREATE TRIGGER handle_updated_at_profiles
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for updated_at on eco_tips
CREATE TRIGGER handle_updated_at_eco_tips
    BEFORE UPDATE ON public.eco_tips
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for updated_at on user_stats
CREATE TRIGGER handle_updated_at_user_stats
    BEFORE UPDATE ON public.user_stats
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for updated_at on achievements
CREATE TRIGGER handle_updated_at_achievements
    BEFORE UPDATE ON public.achievements
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for updated_at on user_achievements
CREATE TRIGGER handle_updated_at_user_achievements
    BEFORE UPDATE ON public.user_achievements
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for updated_at on user_goals
CREATE TRIGGER handle_updated_at_user_goals
    BEFORE UPDATE ON public.user_goals
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Trigger for new user profile creation
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- Trigger for updating user stats on eco tip completion
CREATE TRIGGER on_eco_tip_completed
    AFTER INSERT ON public.user_eco_tip_completions
    FOR EACH ROW
    EXECUTE FUNCTION public.update_user_stats_on_completion();

-- Trigger for checking achievements on user stats update
CREATE TRIGGER on_user_stats_updated
    AFTER UPDATE ON public.user_stats
    FOR EACH ROW
    EXECUTE FUNCTION public.check_achievements();