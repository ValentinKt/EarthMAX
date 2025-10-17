'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { supabase } from '@/lib/supabase';

export const dynamic = 'force-dynamic';

export default function AuthCallbackPage() {
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [message, setMessage] = useState('');
  const router = useRouter();

  useEffect(() => {
    const handleAuthCallback = async () => {
      try {
        const { data, error } = await supabase.auth.getSession();
        
        if (error) {
          setStatus('error');
          setMessage('There was an error confirming your email. Please try again or contact support.');
          return;
        }

        if (data.session) {
          setStatus('success');
          setMessage('Email confirmed successfully! You are now signed in.');
          
          // Redirect to dashboard after 3 seconds
          setTimeout(() => {
            router.push('/dashboard');
          }, 3000);
        } else {
          setStatus('success');
          setMessage('Email confirmed successfully! You can now sign in to your account.');
          
          // Redirect to login after 3 seconds
          setTimeout(() => {
            router.push('/auth/login');
          }, 3000);
        }
        
      } catch {
        setStatus('error');
        setMessage('There was an error confirming your email. Please try again or contact support.');
      }
    };

    handleAuthCallback();
  }, [router]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-teal-50 to-emerald-50 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <Link href="/" className="flex items-center justify-center space-x-2 mb-8">
            <span className="text-3xl">🌍</span>
            <span className="text-3xl font-bold text-teal-800">EarthMAX</span>
          </Link>
          
          {status === 'loading' && (
            <div className="bg-white rounded-xl shadow-lg p-8">
              <div className="flex flex-col items-center space-y-4">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-teal-600"></div>
                <h2 className="text-2xl font-bold text-gray-900">Confirming your email...</h2>
                <p className="text-gray-600">Please wait while we verify your account.</p>
              </div>
            </div>
          )}

          {status === 'success' && (
            <div className="bg-white rounded-xl shadow-lg p-8">
              <div className="flex flex-col items-center space-y-4">
                <div className="text-6xl">✅</div>
                <h2 className="text-2xl font-bold text-gray-900">Email Confirmed!</h2>
                <p className="text-gray-600 text-center">{message}</p>
                <p className="text-sm text-gray-500">Redirecting you to sign in...</p>
                <Link 
                  href="/auth/login"
                  className="bg-teal-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-teal-700 transition-colors"
                >
                  Sign In Now
                </Link>
              </div>
            </div>
          )}

          {status === 'error' && (
            <div className="bg-white rounded-xl shadow-lg p-8">
              <div className="flex flex-col items-center space-y-4">
                <div className="text-6xl">❌</div>
                <h2 className="text-2xl font-bold text-gray-900">Confirmation Failed</h2>
                <p className="text-gray-600 text-center">{message}</p>
                <div className="flex space-x-4">
                  <Link 
                    href="/auth/signup"
                    className="bg-teal-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-teal-700 transition-colors"
                  >
                    Try Again
                  </Link>
                  <Link 
                    href="/"
                    className="border-2 border-teal-600 text-teal-600 px-6 py-3 rounded-lg font-semibold hover:bg-teal-600 hover:text-white transition-colors"
                  >
                    Go Home
                  </Link>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}