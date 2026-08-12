"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import { saveSession } from "@/lib/api";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8081/api/v1";

export default function LoginOfficer() {
  const router = useRouter();
  const [form, setForm] = useState({ loginIdentifier: "", password: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState("");
  const [loading, setLoading] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.loginIdentifier.trim()) e.loginIdentifier = "Email or Employee ID is required.";
    if (!form.password) e.password = "Password is required.";
    return e;
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    setForm({ ...form, [e.target.name]: e.target.value });
    setErrors({ ...errors, [e.target.name]: "" });
    setServerError("");
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) { setErrors(errs); return; }

    setLoading(true);
    try {
      const res = await fetch(`${BASE_URL}/auth/login/employee`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.message || "Invalid credentials.");

      saveSession(data);
      router.push("/officer/dashboard");
    } catch (err: unknown) {
      setServerError(err instanceof Error ? err.message : "Login failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <Navbar />

      <main className="flex-1 flex items-center justify-center px-4 py-16">
        <div className="bg-white rounded-lg shadow-md w-full max-w-md">
          {/* Header */}
          <div className="border-b border-gray-100 px-8 py-6">
            <h1 className="text-2xl font-bold text-gray-800">GRAMS Portal Sign In</h1>
            <p className="text-sm text-gray-500 mt-1">Sign in with your Email, Phone Number, or Fayda ID.</p>
          </div>

          <form onSubmit={handleSubmit} noValidate className="px-8 py-8 space-y-5">
            {serverError && (
              <div className="bg-red-50 border border-red-300 text-red-600 text-sm px-4 py-3 rounded">
                {serverError}
              </div>
            )}

            {/* Login Identifier */}
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700">
                Email / Phone / Fayda ID <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="loginIdentifier"
                value={form.loginIdentifier}
                onChange={handleChange}
                placeholder="Enter email, phone, or Fayda ID"
                className={`w-full border rounded px-3 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-400 transition ${
                  errors.loginIdentifier ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"
                }`}
              />
              {errors.loginIdentifier && <p className="text-xs text-red-500">{errors.loginIdentifier}</p>}
            </div>

            {/* Password */}
            <div className="flex flex-col gap-1.5">
              <label className="text-sm font-medium text-gray-700">
                Password <span className="text-red-500">*</span>
              </label>
              <input
                type="password"
                name="password"
                value={form.password}
                onChange={handleChange}
                placeholder="Enter your password"
                className={`w-full border rounded px-3 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-400 transition ${
                  errors.password ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"
                }`}
              />
              {errors.password && <p className="text-xs text-red-500">{errors.password}</p>}
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-gray-900 text-white font-bold text-sm tracking-widest uppercase rounded hover:bg-gray-800 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {loading ? "Signing in..." : "Login"}
              </button>

              <p className="text-center text-sm text-gray-500 mt-4">
                Don&apos;t have an account?{" "}
                <Link href="/register" className="text-green-500 font-semibold hover:underline">
                  Register as Citizen
                </Link>
              </p>
            </div>
          </form>
        </div>
      </main>

      <Footer />
    </div>
  );
}
