'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { supabase } from '@/lib/supabase';
import { User } from '@supabase/supabase-js';

export const dynamic = 'force-dynamic';

export default function DashboardPage() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const getUser = async () => {
      const { data: { session } } = await supabase.auth.getSession();
      
      if (!session) {
        router.push('/auth/login');
        return;
      }
      
      setUser(session.user);
      setLoading(false);
    };

    getUser();

    const { data: { subscription } } = supabase.auth.onAuthStateChange(
      (event, session) => {
        if (event === 'SIGNED_OUT' || !session) {
          router.push('/auth/login');
        } else {
          setUser(session.user);
        }
      }
    );

    return () => subscription.unsubscribe();
  }, [router]);

  const handleSignOut = async () => {
    await supabase.auth.signOut();
    router.push('/');
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-teal-50 to-emerald-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-teal-50 to-emerald-50">
      <nav className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <Link href="/" className="flex items-center space-x-2">
                <span className="text-2xl">🌍</span>
                <span className="text-xl font-bold text-teal-800">EarthMAX</span>
              </Link>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-gray-700">Welcome, {user?.email}</span>
              <button
                onClick={handleSignOut}
                className="bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 transition-colors"
              >
                Sign Out
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <div className="px-4 py-6 sm:px-0">
          <div className="bg-white rounded-xl shadow-lg p-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-6">
              🌱 Welcome to Your EarthMAX Dashboard
            </h1>
            
            <div className="grid md:grid-cols-3 gap-6 mb-8">
              <div className="bg-teal-50 rounded-lg p-6">
                <div className="text-3xl mb-2">📊</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Carbon Footprint</h3>
                <p className="text-gray-600">Track your environmental impact</p>
                <div className="mt-4">
                  <span className="text-2xl font-bold text-teal-600">0 kg CO₂</span>
                  <p className="text-sm text-gray-500">This month</p>
                </div>
              </div>
              
              <div className="bg-emerald-50 rounded-lg p-6">
                <div className="text-3xl mb-2">🎯</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Goals</h3>
                <p className="text-gray-600">Set and achieve sustainability goals</p>
                <div className="mt-4">
                  <span className="text-2xl font-bold text-emerald-600">0/5</span>
                  <p className="text-sm text-gray-500">Goals completed</p>
                </div>
              </div>
              
              <div className="bg-blue-50 rounded-lg p-6">
                <div className="text-3xl mb-2">🏆</div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">Achievements</h3>
                <p className="text-gray-600">Unlock eco-friendly milestones</p>
                <div className="mt-4">
                  <span className="text-2xl font-bold text-blue-600">0</span>
                  <p className="text-sm text-gray-500">Badges earned</p>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 rounded-lg p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Getting Started</h2>
              <div className="space-y-3">
                <div className="flex items-center space-x-3">
                  <div className="w-6 h-6 bg-teal-600 rounded-full flex items-center justify-center">
                    <span className="text-white text-sm">✓</span>
                  </div>
                  <span className="text-gray-700">Account created successfully</span>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-6 h-6 bg-gray-300 rounded-full flex items-center justify-center">
                    <span className="text-gray-600 text-sm">2</span>
                  </div>
                  <span className="text-gray-700">Set your first sustainability goal</span>
                </div>
                <div className="flex items-center space-x-3">
                  <div className="w-6 h-6 bg-gray-300 rounded-full flex items-center justify-center">
                    <span className="text-gray-600 text-sm">3</span>
                  </div>
                  <span className="text-gray-700">Start tracking your carbon footprint</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}