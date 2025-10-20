-- EarthMAX Supabase Database Setup
-- Run this script in your Supabase SQL editor to create the necessary tables

-- Enable Row Level Security
ALTER DATABASE postgres SET "app.jwt_secret" TO 'your-jwt-secret';

-- Create profiles table (extends auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    bio TEXT,
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create events table
CREATE TABLE IF NOT EXISTS public.events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    date TEXT NOT NULL,
    location TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    organizer_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    organizer_name TEXT NOT NULL,
    image_url TEXT,
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    category TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create event_participants table (many-to-many relationship)
CREATE TABLE IF NOT EXISTS public.event_participants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    event_id UUID REFERENCES public.events(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(event_id, user_id)
);

-- Create todo_items table
CREATE TABLE IF NOT EXISTS public.todo_items (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    event_id UUID REFERENCES public.events(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    is_completed BOOLEAN DEFAULT FALSE,
    assigned_to UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    created_by UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.event_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.todo_items ENABLE ROW LEVEL SECURITY;

-- Profiles policies
CREATE POLICY "Public profiles are viewable by everyone" ON public.profiles
    FOR SELECT USING (true);

CREATE POLICY "Users can insert their own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

-- Events policies
CREATE POLICY "Events are viewable by everyone" ON public.events
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can create events" ON public.events
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Users can update their own events" ON public.events
    FOR UPDATE USING (auth.uid() = organizer_id);

CREATE POLICY "Users can delete their own events" ON public.events
    FOR DELETE USING (auth.uid() = organizer_id);

-- Event participants policies
CREATE POLICY "Event participants are viewable by everyone" ON public.event_participants
    FOR SELECT USING (true);

CREATE POLICY "Authenticated users can join events" ON public.event_participants
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Users can leave events they joined" ON public.event_participants
    FOR DELETE USING (auth.uid() = user_id);

-- Todo items policies
CREATE POLICY "Todo items are viewable by event participants" ON public.todo_items
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.event_participants 
            WHERE event_id = todo_items.event_id 
            AND user_id = auth.uid()
        ) OR 
        EXISTS (
            SELECT 1 FROM public.events 
            WHERE id = todo_items.event_id 
            AND organizer_id = auth.uid()
        )
    );

CREATE POLICY "Event participants can create todo items" ON public.todo_items
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.event_participants 
            WHERE event_id = todo_items.event_id 
            AND user_id = auth.uid()
        ) OR 
        EXISTS (
            SELECT 1 FROM public.events 
            WHERE id = todo_items.event_id 
            AND organizer_id = auth.uid()
        )
    );

CREATE POLICY "Users can update todo items they created or are assigned to" ON public.todo_items
    FOR UPDATE USING (
        auth.uid() = created_by OR 
        auth.uid() = assigned_to OR
        EXISTS (
            SELECT 1 FROM public.events 
            WHERE id = todo_items.event_id 
            AND organizer_id = auth.uid()
        )
    );

CREATE POLICY "Users can delete todo items they created or event organizers can delete" ON public.todo_items
    FOR DELETE USING (
        auth.uid() = created_by OR
        EXISTS (
            SELECT 1 FROM public.events 
            WHERE id = todo_items.event_id 
            AND organizer_id = auth.uid()
        )
    );

-- Create function to handle user profile creation
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email, display_name)
    VALUES (NEW.id, NEW.email, COALESCE(NEW.raw_user_meta_data->>'display_name', ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger to automatically create profile on user signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER handle_updated_at BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at BEFORE UPDATE ON public.events
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

CREATE TRIGGER handle_updated_at BEFORE UPDATE ON public.todo_items
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Insert sample data (optional)
INSERT INTO public.events (title, description, date, location, latitude, longitude, organizer_id, organizer_name, category) VALUES
('Beach Cleanup', 'Join us for a community beach cleanup event', '2024-02-15T10:00:00Z', 'Santa Monica Beach', 34.0195, -118.4912, '00000000-0000-0000-0000-000000000000', 'EarthMAX Team', 'cleanup'),
('Tree Planting', 'Help us plant trees in the local park', '2024-02-20T09:00:00Z', 'Central Park', 40.7829, -73.9654, '00000000-0000-0000-0000-000000000000', 'EarthMAX Team', 'conservation'),
('Recycling Workshop', 'Learn about proper recycling techniques', '2024-02-25T14:00:00Z', 'Community Center', 34.0522, -118.2437, '00000000-0000-0000-0000-000000000000', 'EarthMAX Team', 'education')
ON CONFLICT DO NOTHING;