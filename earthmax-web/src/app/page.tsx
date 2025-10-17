import Image from "next/image";
import Link from "next/link";

export default function Home() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-teal-50 to-emerald-50">
      {/* Header */}
      <header className="bg-white/80 backdrop-blur-sm border-b border-teal-100 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-2">
              <span className="text-2xl">🌍</span>
              <span className="text-2xl font-bold text-teal-800">EarthMAX</span>
            </div>
            <nav className="hidden md:flex space-x-8">
              <Link href="#features" className="text-teal-700 hover:text-teal-900 font-medium">
                Features
              </Link>
              <Link href="#about" className="text-teal-700 hover:text-teal-900 font-medium">
                About
              </Link>
              <Link href="/auth/login" className="bg-teal-600 text-white px-4 py-2 rounded-lg hover:bg-teal-700 transition-colors">
                Get Started
              </Link>
            </nav>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Sustainable Living
            <span className="block text-teal-600">Made Simple</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-3xl mx-auto">
            Join our community of eco-conscious individuals working together to create a more sustainable future. 
            Track your environmental impact, connect with like-minded people, and make a difference.
          </p>
          
          <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <Link 
              href="/auth/signup"
              className="bg-teal-600 text-white px-8 py-4 rounded-lg text-lg font-semibold hover:bg-teal-700 transition-colors shadow-lg hover:shadow-xl transform hover:-translate-y-1"
            >
              🌱 Start Your Journey
            </Link>
            <Link 
              href="/auth/login"
              className="border-2 border-teal-600 text-teal-600 px-8 py-4 rounded-lg text-lg font-semibold hover:bg-teal-600 hover:text-white transition-colors"
            >
              Sign In
            </Link>
          </div>
        </div>

        {/* Features Section */}
        <section id="features" className="mt-24">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
            Why Choose EarthMAX?
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <div className="bg-white rounded-xl p-8 shadow-lg hover:shadow-xl transition-shadow">
              <div className="text-4xl mb-4">📊</div>
              <h3 className="text-xl font-semibold text-gray-900 mb-3">Track Your Impact</h3>
              <p className="text-gray-600">
                Monitor your environmental footprint and see the positive changes you're making for our planet.
              </p>
            </div>
            <div className="bg-white rounded-xl p-8 shadow-lg hover:shadow-xl transition-shadow">
              <div className="text-4xl mb-4">🤝</div>
              <h3 className="text-xl font-semibold text-gray-900 mb-3">Connect & Share</h3>
              <p className="text-gray-600">
                Join events, share tips, and connect with a community that cares about sustainability.
              </p>
            </div>
            <div className="bg-white rounded-xl p-8 shadow-lg hover:shadow-xl transition-shadow">
              <div className="text-4xl mb-4">🎯</div>
              <h3 className="text-xl font-semibold text-gray-900 mb-3">Achieve Goals</h3>
              <p className="text-gray-600">
                Set and achieve sustainability goals with personalized recommendations and challenges.
              </p>
            </div>
          </div>
        </section>

        {/* About Section */}
        <section id="about" className="mt-24 bg-white rounded-2xl p-12 shadow-lg">
          <div className="text-center">
            <h2 className="text-3xl font-bold text-gray-900 mb-6">
              Building a Sustainable Future Together
            </h2>
            <p className="text-lg text-gray-600 max-w-4xl mx-auto">
              EarthMAX is more than just an app – it's a movement. We believe that small actions, 
              when multiplied by millions of people, can transform the world. Join us in creating 
              a more sustainable future for generations to come.
            </p>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-teal-800 text-white mt-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
          <div className="grid md:grid-cols-4 gap-8">
            <div>
              <div className="flex items-center space-x-2 mb-4">
                <span className="text-2xl">🌍</span>
                <span className="text-xl font-bold">EarthMAX</span>
              </div>
              <p className="text-teal-200">
                Sustainable living made simple for everyone.
              </p>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Product</h3>
              <ul className="space-y-2 text-teal-200">
                <li><Link href="#features" className="hover:text-white">Features</Link></li>
                <li><Link href="/auth/signup" className="hover:text-white">Sign Up</Link></li>
                <li><Link href="/auth/login" className="hover:text-white">Login</Link></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Support</h3>
              <ul className="space-y-2 text-teal-200">
                <li><Link href="mailto:support@earthmax.app" className="hover:text-white">Contact</Link></li>
                <li><Link href="/privacy" className="hover:text-white">Privacy Policy</Link></li>
                <li><Link href="/terms" className="hover:text-white">Terms of Service</Link></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold mb-4">Download App</h3>
              <p className="text-teal-200 mb-4">
                Get the full EarthMAX experience on your mobile device.
              </p>
              <div className="space-y-2">
                <div className="bg-teal-700 px-4 py-2 rounded-lg text-sm">
                  📱 Android App Coming Soon
                </div>
              </div>
            </div>
          </div>
          <div className="border-t border-teal-700 mt-8 pt-8 text-center text-teal-200">
            <p>&copy; 2024 EarthMAX. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
