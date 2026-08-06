"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getSession, clearSession } from "@/lib/api";
import type { UserSummary } from "@/lib/api";

export default function CitizenDashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<UserSummary | null>(null);

  useEffect(() => {
    const session = getSession();
    if (!session) { router.push("/citizen/login"); return; }
    if (session.user.userType !== "CITIZEN") { router.push("/citizen/login"); return; }
    setUser(session.user);
  }, [router]);

  function handleLogout() {
    clearSession();
    router.push("/");
  }

  if (!user) return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-gray-500 text-sm">Loading...</div>
    </div>
  );

  const initials = `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
  const roleLabel = user.roles?.[0] ?? "CITIZEN";

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* Top bar */}
      <header className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <Link href="/" className="text-green-500 text-sm font-semibold tracking-widest uppercase">
          Rental System
        </Link>
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-green-500 flex items-center justify-center text-white text-sm font-bold">
              {initials}
            </div>
            <span className="text-sm font-semibold text-gray-700">{user.firstName} {user.lastName}</span>
          </div>
          <button onClick={handleLogout}
            className="px-4 py-1.5 text-xs font-semibold text-red-500 border border-red-400 rounded hover:bg-red-50 transition-colors uppercase tracking-wider">
            Logout
          </button>
        </div>
      </header>

      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-10">
        {/* Welcome */}
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-gray-800">
            Welcome, {user.firstName} {user.lastName}
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Your GRAMS Citizen Dashboard — manage your rental agreements and profile.
          </p>
        </div>

        {/* Profile card */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white rounded-lg border border-gray-200 p-5 shadow-sm">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-1">Role</p>
            <span className="inline-block bg-green-100 text-green-700 text-xs font-bold px-3 py-1 rounded-full uppercase tracking-wide">
              {roleLabel}
            </span>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-5 shadow-sm">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-1">Email</p>
            <p className="text-sm font-medium text-gray-700 truncate">{user.email}</p>
          </div>
          <div className="bg-white rounded-lg border border-gray-200 p-5 shadow-sm">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-1">Phone</p>
            <p className="text-sm font-medium text-gray-700">{user.phoneNumber}</p>
          </div>
        </div>

        {/* Action cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <ActionCard
            title="My Rental Agreements"
            desc="View, sign, and manage your rental contracts."
            icon="📄"
            href="#"
          />
          <ActionCard
            title="Properties"
            desc="Browse available rental properties in your area."
            icon="🏠"
            href="#"
          />
          <ActionCard
            title="Payments"
            desc="View payment history and upcoming dues."
            icon="💳"
            href="#"
          />
          <ActionCard
            title="My Profile"
            desc="Update your personal information and credentials."
            icon="👤"
            href="#"
          />
        </div>
      </main>
    </div>
  );
}

function ActionCard({ title, desc, icon, href }: { title: string; desc: string; icon: string; href: string }) {
  return (
    <Link href={href}
      className="bg-white rounded-lg border border-gray-200 p-6 shadow-sm flex items-start gap-4 hover:border-green-400 hover:shadow-md transition-all">
      <div className="text-3xl">{icon}</div>
      <div>
        <h3 className="font-semibold text-gray-800 mb-1">{title}</h3>
        <p className="text-sm text-gray-500">{desc}</p>
      </div>
    </Link>
  );
}
