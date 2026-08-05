"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function Navbar() {
  const [username, setUsername] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem("jwt");
    const user = localStorage.getItem("username");
    if (token && user) setUsername(user);
  }, []);

  function handleLogout() {
    localStorage.removeItem("jwt");
    localStorage.removeItem("username");
    setUsername(null);
    router.push("/");
  }

  return (
    <nav className="w-full bg-white border-b border-gray-200">
      <div className="max-w-6xl mx-auto px-8 py-4 flex items-center justify-between">
        {/* Logo */}
        <Link href="/" className="text-green-500 text-sm font-semibold tracking-widest uppercase">
          Rental System
        </Link>

        {/* Right side */}
        {username ? (
          <div className="flex items-center gap-4">
            {/* Profile avatar + username */}
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-green-500 flex items-center justify-center text-white text-sm font-bold uppercase">
                {username.charAt(0)}
              </div>
              <span className="text-sm font-semibold text-gray-700">{username}</span>
            </div>
            <button
              onClick={handleLogout}
              className="px-5 py-1.5 text-sm font-semibold text-red-500 border border-red-400 rounded hover:bg-red-50 transition-colors tracking-wider uppercase"
            >
              Logout
            </button>
          </div>
        ) : (
          <div className="flex items-center gap-3">
            <Link
              href="/login"
              className="px-6 py-2 text-sm font-bold text-green-500 border-2 border-green-500 rounded tracking-widest uppercase hover:bg-green-50 transition-colors"
            >
              Login
            </Link>
            <Link
              href="/register"
              className="px-6 py-2 text-sm font-bold text-white bg-gray-900 rounded tracking-widest uppercase hover:bg-gray-800 transition-colors"
            >
              Signup
            </Link>
          </div>
        )}
      </div>
    </nav>
  );
}
