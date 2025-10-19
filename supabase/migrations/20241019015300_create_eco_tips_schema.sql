-- Migration: Create EcoTips Schema
-- This migration creates all tables and types needed for the GraphQL schema

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create custom types/enums
CREATE TYPE difficulty_level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED');
CREATE TYPE impact_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH');
CREATE TYPE resource_type AS ENUM ('ARTICLE', 'VIDEO', 'INFOGRAPHIC', 'TOOL', 'CALCULATOR');
CREATE TYPE achievement_category AS ENUM ('ENVIRONMENTAL', 'SOCIAL', 'EDUCATIONAL', 'MILESTONE');
CREATE TYPE eco_tip_sort_field AS ENUM ('CREATED_AT', 'UPDATED_AT', 'TITLE', 'DIFFICULTY', 'ESTIMATED_IMPACT');
CREATE TYPE sort_order AS ENUM ('ASC', 'DESC');
CREATE TYPE error_code AS ENUM ('VALIDATION_ERROR', 'NOT_FOUND', 'UNAUTHORIZED', 'INTERNAL_ERROR');

-- Create profiles table (extends auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    bio TEXT,
    avatar_url TEXT,
    location TEXT,
    preferences JSONB DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create eco_tips table
CREATE TABLE IF NOT EXISTS public.eco_tips (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT NOT NULL,
    difficulty difficulty_level NOT NULL DEFAULT 'BEGINNER',
    estimated_impact impact_level NOT NULL DEFAULT 'MEDIUM',
    steps TEXT[] NOT NULL DEFAULT '{}',
    image_url TEXT,
    tags TEXT[] DEFAULT '{}',
    resources JSONB DEFAULT '[]',
    carbon_savings_kg DECIMAL(10,2),
    cost_savings_usd DECIMAL(10,2),
    time_required_minutes INTEGER,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create user_stats table
CREATE TABLE IF NOT EXISTS public.user_stats (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL UNIQUE,
    completed_tips_count INTEGER DEFAULT 0,
    total_carbon_saved_kg DECIMAL(10,2) DEFAULT 0,
    total_cost_saved_usd DECIMAL(10,2) DEFAULT 0,
    current_streak INTEGER DEFAULT 0,
    longest_streak INTEGER DEFAULT 0,
    last_activity_date DATE,
    level INTEGER DEFAULT 1,
    experience_points INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create achievements table
CREATE TABLE IF NOT EXISTS public.achievements (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category achievement_category NOT NULL,
    icon_url TEXT,
    points INTEGER DEFAULT 0,
    requirements JSONB NOT NULL DEFAULT '{}',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create user_achievements table (many-to-many)
CREATE TABLE IF NOT EXISTS public.user_achievements (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    achievement_id UUID REFERENCES public.achievements(id) ON DELETE CASCADE NOT NULL,
    progress INTEGER DEFAULT 0,
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(user_id, achievement_id)
);

-- Create user_eco_tip_completions table
CREATE TABLE IF NOT EXISTS public.user_eco_tip_completions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    eco_tip_id UUID REFERENCES public.eco_tips(id) ON DELETE CASCADE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    notes TEXT,
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    carbon_saved_kg DECIMAL(10,2),
    cost_saved_usd DECIMAL(10,2),
    UNIQUE(user_id, eco_tip_id)
);

-- Create user_goals table
CREATE TABLE IF NOT EXISTS public.user_goals (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    target_value DECIMAL(10,2) NOT NULL,
    current_value DECIMAL(10,2) DEFAULT 0,
    unit TEXT NOT NULL, -- 'kg_co2', 'usd', 'tips_completed', etc.
    target_date DATE,
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create recent_activities table
CREATE TABLE IF NOT EXISTS public.recent_activities (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    activity_type TEXT NOT NULL, -- 'tip_completed', 'achievement_unlocked', 'goal_reached', etc.
    activity_data JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_eco_tips_category ON public.eco_tips(category);
CREATE INDEX IF NOT EXISTS idx_eco_tips_difficulty ON public.eco_tips(difficulty);
CREATE INDEX IF NOT EXISTS idx_eco_tips_active ON public.eco_tips(is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_eco_tips_created_at ON public.eco_tips(created_at);
CREATE INDEX IF NOT EXISTS idx_eco_tips_tags ON public.eco_tips USING GIN(tags);

CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON public.user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_completed ON public.user_achievements(is_completed);

CREATE INDEX IF NOT EXISTS idx_user_completions_user_id ON public.user_eco_tip_completions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_completions_completed_at ON public.user_eco_tip_completions(completed_at);

CREATE INDEX IF NOT EXISTS idx_recent_activities_user_id ON public.recent_activities(user_id);
CREATE INDEX IF NOT EXISTS idx_recent_activities_created_at ON public.recent_activities(created_at);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.eco_tips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_eco_tip_completions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_goals ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recent_activities ENABLE ROW LEVEL SECURITY;