"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

export default function OfficerDashboardPage() {
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
      if (userData.userType !== "GOVERNMENT_EMPLOYEE" && !userData.governmentEmployee) {
        router.push("/unauthorized");
        return;
      }
      setUser(userData);
    } catch {
      router.push("/login");
    }
  }, [router]);

  if (!user) return <div className="p-8">Loading Officer Portal...</div>;

  return (
    <div className="min-h-screen bg-slate-100 p-8">
      <div className="max-w-5xl mx-auto bg-white rounded-lg shadow p-6">
        <div className="border-b pb-4 mb-6 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Government Officer Portal</h1>
            <p className="text-sm text-slate-500">Official Government Verification & Compliance Dashboard</p>
          </div>
          <span className="bg-blue-100 text-blue-800 text-xs font-semibold px-3 py-1 rounded-full">
            Officer Domain: {user.subCityCode || "City Level"} / {user.woredaCode || "Woreda Wide"}
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-slate-50 p-4 rounded border">
            <h3 className="text-sm font-semibold text-slate-600">Employee Number</h3>
            <p className="text-lg font-bold text-slate-800">{user.employeeNumber || "N/A"}</p>
          </div>
          <div className="bg-slate-50 p-4 rounded border">
            <h3 className="text-sm font-semibold text-slate-600">Department</h3>
            <p className="text-lg font-bold text-slate-800">{user.department || "N/A"}</p>
          </div>
          <div className="bg-slate-50 p-4 rounded border">
            <h3 className="text-sm font-semibold text-slate-600">Assigned Roles</h3>
            <p className="text-sm font-medium text-blue-600">{user.roles?.join(", ") || "OFFICER"}</p>
          </div>
        </div>

        <div className="bg-amber-50 border border-amber-200 p-4 rounded text-sm text-amber-800">
          <strong>Security Notice:</strong> Access to government records is logged and restricted according to officer jurisdiction.
        </div>
      </div>
    </div>
  );
}
