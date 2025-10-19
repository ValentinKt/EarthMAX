-- Migration: Seed Initial Data
-- This migration adds initial eco tips and achievements

-- Insert sample eco tips
INSERT INTO public.eco_tips (
    title, 
    description, 
    category, 
    difficulty, 
    estimated_impact, 
    steps, 
    tags, 
    carbon_savings_kg, 
    cost_savings_usd, 
    time_required_minutes,
    resources
) VALUES 
(
    'Switch to LED Light Bulbs',
    'Replace traditional incandescent bulbs with energy-efficient LED bulbs to reduce energy consumption and save money.',
    'Energy',
    'BEGINNER',
    'MEDIUM',
    ARRAY[
        'Identify all incandescent bulbs in your home',
        'Purchase LED replacements with equivalent brightness',
        'Turn off power and replace bulbs one by one',
        'Dispose of old bulbs properly at recycling center'
    ],
    ARRAY['energy', 'lighting', 'savings', 'easy'],
    50.0,
    75.0,
    30,
    '[
        {
            "type": "ARTICLE",
            "title": "LED vs Incandescent: Complete Guide",
            "url": "https://example.com/led-guide",
            "description": "Comprehensive comparison of LED and traditional bulbs"
        }
    ]'::jsonb
),
(
    'Start Composting Kitchen Scraps',
    'Create nutrient-rich compost from kitchen waste while reducing landfill contributions.',
    'Waste Reduction',
    'INTERMEDIATE',
    'HIGH',
    ARRAY[
        'Choose a composting method (bin, tumbler, or pile)',
        'Set up your composting area in yard or balcony',
        'Collect fruit and vegetable scraps, coffee grounds, eggshells',
        'Layer green materials with brown materials (leaves, paper)',
        'Turn compost regularly and maintain moisture',
        'Use finished compost in garden after 3-6 months'
    ],
    ARRAY['composting', 'waste', 'gardening', 'organic'],
    120.0,
    200.0,
    45,
    '[
        {
            "type": "VIDEO",
            "title": "Composting for Beginners",
            "url": "https://example.com/composting-video",
            "description": "Step-by-step video guide to home composting"
        },
        {
            "type": "TOOL",
            "title": "Compost Calculator",
            "url": "https://example.com/compost-calc",
            "description": "Calculate your composting impact"
        }
    ]'::jsonb
),
(
    'Reduce Shower Time by 2 Minutes',
    'Save water and energy by taking shorter showers without compromising cleanliness.',
    'Water Conservation',
    'BEGINNER',
    'MEDIUM',
    ARRAY[
        'Time your current shower length',
        'Set a timer for 2 minutes less than usual',
        'Focus on essential washing tasks',
        'Turn off water while soaping or shampooing',
        'Track your progress for a week'
    ],
    ARRAY['water', 'energy', 'habits', 'daily'],
    25.0,
    40.0,
    5,
    '[
        {
            "type": "CALCULATOR",
            "title": "Water Savings Calculator",
            "url": "https://example.com/water-calc",
            "description": "Calculate water and cost savings from shorter showers"
        }
    ]'::jsonb
),
(
    'Use Public Transportation Once a Week',
    'Reduce carbon emissions by replacing one car trip per week with public transit.',
    'Transportation',
    'BEGINNER',
    'HIGH',
    ARRAY[
        'Research public transit routes for common destinations',
        'Download transit apps for schedules and planning',
        'Choose one regular car trip to replace',
        'Plan extra time for transit journey',
        'Track your weekly public transit usage'
    ],
    ARRAY['transportation', 'carbon', 'public-transit', 'weekly'],
    80.0,
    25.0,
    15,
    '[
        {
            "type": "ARTICLE",
            "title": "Benefits of Public Transportation",
            "url": "https://example.com/transit-benefits",
            "description": "Environmental and economic benefits of public transit"
        }
    ]'::jsonb
),
(
    'Create a Home Recycling System',
    'Set up an organized recycling system to properly sort and dispose of recyclable materials.',
    'Waste Reduction',
    'INTERMEDIATE',
    'HIGH',
    ARRAY[
        'Research local recycling guidelines and accepted materials',
        'Set up separate containers for different recyclables',
        'Label containers clearly (paper, plastic, glass, metal)',
        'Create a cleaning routine for recyclables',
        'Establish a regular pickup or drop-off schedule',
        'Educate household members on proper sorting'
    ],
    ARRAY['recycling', 'waste', 'organization', 'system'],
    200.0,
    150.0,
    60,
    '[
        {
            "type": "INFOGRAPHIC",
            "title": "Recycling Guide by Material",
            "url": "https://example.com/recycling-guide",
            "description": "Visual guide to recycling different materials"
        }
    ]'::jsonb
);

-- Insert sample achievements
INSERT INTO public.achievements (
    title,
    description,
    category,
    icon_url,
    points,
    requirements
) VALUES
(
    'First Steps',
    'Complete your first eco-friendly tip',
    'MILESTONE',
    'https://example.com/icons/first-steps.svg',
    10,
    '{"type": "tips_completed", "target": 1}'::jsonb
),
(
    'Eco Warrior',
    'Complete 10 eco-friendly tips',
    'MILESTONE',
    'https://example.com/icons/eco-warrior.svg',
    50,
    '{"type": "tips_completed", "target": 10}'::jsonb
),
(
    'Green Champion',
    'Complete 25 eco-friendly tips',
    'MILESTONE',
    'https://example.com/icons/green-champion.svg',
    100,
    '{"type": "tips_completed", "target": 25}'::jsonb
),
(
    'Streak Master',
    'Maintain a 7-day streak of eco activities',
    'MILESTONE',
    'https://example.com/icons/streak-master.svg',
    75,
    '{"type": "streak", "target": 7}'::jsonb
),
(
    'Carbon Saver',
    'Save 100kg of CO2 through eco activities',
    'ENVIRONMENTAL',
    'https://example.com/icons/carbon-saver.svg',
    100,
    '{"type": "carbon_saved", "target": 100}'::jsonb
),
(
    'Climate Hero',
    'Save 500kg of CO2 through eco activities',
    'ENVIRONMENTAL',
    'https://example.com/icons/climate-hero.svg',
    250,
    '{"type": "carbon_saved", "target": 500}'::jsonb
),
(
    'Knowledge Seeker',
    'Read 5 educational resources',
    'EDUCATIONAL',
    'https://example.com/icons/knowledge-seeker.svg',
    30,
    '{"type": "resources_read", "target": 5}'::jsonb
),
(
    'Community Builder',
    'Share 3 eco tips with friends',
    'SOCIAL',
    'https://example.com/icons/community-builder.svg',
    40,
    '{"type": "tips_shared", "target": 3}'::jsonb
);