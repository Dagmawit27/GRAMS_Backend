"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function AdminDashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<any>(null);

  useEffect(() => {
    const userStr = localStorage.getItem("user");
    if (!userStr) {
      router.push("/login");
      return;
    }
    try {
      const userData = JSON.parse(userStr);
      const hasAdminRole = userData.roles?.includes("SYSTEM_ADMINISTRATOR") || userData.roles?.includes("ROLE_SYSTEM_ADMINISTRATOR");
      if (!hasAdminRole) {
        router.push("/unauthorized");
        return;
      }
      setUser(userData);
    } catch {
      router.push("/login");
    }
  }, [router]);

  if (!user) return <div className="p-8">Loading System Admin Portal...</div>;

  return (
    <div className="min-h-screen bg-slate-900 text-white p-8">
      <div className="max-w-5xl mx-auto bg-slate-800 rounded-lg shadow-xl p-6 border border-slate-700">
        <div className="border-b border-slate-700 pb-4 mb-6 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-emerald-400">System Administrator Portal</h1>
            <p className="text-sm text-slate-400">GRAMS Platform Administration & Privileged Governance</p>
          </div>
          <span className="bg-emerald-900 text-emerald-300 text-xs font-semibold px-3 py-1 rounded-full border border-emerald-700">
            Root System Access
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div className="bg-slate-700/50 p-4 rounded border border-slate-600">
            <h3 className="text-sm font-semibold text-slate-300">Admin Account</h3>
            <p className="text-lg font-bold text-white">{user.email}</p>
          </div>
          <div className="bg-slate-700/50 p-4 rounded border border-slate-600">
            <h3 className="text-sm font-semibold text-slate-300">Privileges</h3>
            <p className="text-sm font-medium text-emerald-400">Government Employee Registration & System Config</p>
          </div>
        </div>

        <div className="bg-slate-900 p-4 rounded border border-slate-700 text-sm text-slate-300">
          <p className="font-semibold text-emerald-400 mb-1">Administrative Actions Available:</p>
          <ul className="list-disc pl-5 space-y-1">
            <li>Register Government Officers (Woreda Officers, Supervisors, Sub-City Admins)</li>
            <li>Manage Role & Jurisdiction Access Policies</li>
            <li>View System Security Audit Logs</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
