"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { registerCitizen, saveSession } from "@/lib/api";

export default function CitizenRegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({
    faydaId: "",
    firstName: "",
    middleName: "",
    lastName: "",
    gender: "" as "MALE" | "FEMALE" | "",
    dateOfBirth: "",
    phoneNumber: "",
    email: "",
    password: "",
    confirmPassword: "",
    rolePreference: "CITIZEN",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState("");
  const [loading, setLoading] = useState(false);

  function validate() {
    const e: Record<string, string> = {};
    if (!form.faydaId.trim()) e.faydaId = "Fayda ID is required.";
    else if (isNaN(Number(form.faydaId))) e.faydaId = "Fayda ID must be a number.";
    if (!form.firstName.trim()) e.firstName = "First name is required.";
    if (!form.lastName.trim()) e.lastName = "Last name is required.";
    if (!form.gender) e.gender = "Gender is required.";
    if (!form.dateOfBirth) e.dateOfBirth = "Date of birth is required.";
    if (!form.phoneNumber.trim()) e.phoneNumber = "Phone number is required.";
    else if (!/^\+?[0-9]{9,15}$/.test(form.phoneNumber)) e.phoneNumber = "Invalid phone number.";
    if (!form.email.trim()) e.email = "Email is required.";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "Invalid email address.";
    if (!form.password) e.password = "Password is required.";
    else if (form.password.length < 8) e.password = "Password must be at least 8 characters.";
    if (!form.confirmPassword) e.confirmPassword = "Please confirm your password.";
    else if (form.password !== form.confirmPassword) e.confirmPassword = "Passwords do not match.";
    return e;
  }

  function handleChange(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
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
      console.log(form);
      const result = await registerCitizen({
        faydaId: Number(form.faydaId),
        firstName: form.firstName,
        middleName: form.middleName || undefined,
        lastName: form.lastName,
        gender: form.gender as "MALE" | "FEMALE",
        dateOfBirth: form.dateOfBirth,
        phoneNumber: form.phoneNumber,
        email: form.email,
        password: form.password,
        rolePreference: form.rolePreference || undefined,
      });
      saveSession(result);
      router.push("/citizen/dashboard");
    } catch (err: unknown) {
      setServerError(err instanceof Error ? err.message : "Registration failed.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <div className="bg-white border-b border-gray-200 px-8 py-4 flex items-center justify-between">
        <Link href="/citizen" className="text-green-500 text-sm font-semibold tracking-widest uppercase">
          Rental System
        </Link>
        <span className="text-sm text-gray-500">
          Already have an account?{" "}
          <Link href="/citizen/login" className="text-green-500 font-semibold hover:underline">Login</Link>
        </span>
      </div>

      <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100 flex-1 flex items-start justify-center py-10 px-4">
        <div className="bg-white rounded-lg shadow-md w-full max-w-2xl">
          <div className="bg-gradient-to-r from-green-600 to-green-500 px-8 py-6 border-b border-gray-100">
            <h1 className="text-2xl font-bold text-white">Citizen Registration</h1>
            <p className="text-green-50 text-sm mt-1">Create your GRAMS citizen account.</p>
          </div>

          <form onSubmit={handleSubmit} noValidate className="px-8 py-8 space-y-8">
            {serverError && (
              <div className="bg-red-50 border-l-4 border-red-500 text-red-700 px-4 py-3 rounded text-sm">
                {serverError}
              </div>
            )}

            {/* Personal Information */}
            <section>
              <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">Personal Information</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <Field label="Fayda ID" name="faydaId" value={form.faydaId} onChange={handleChange} error={errors.faydaId} placeholder="Your national Fayda ID" />
                <Field label="First Name" name="firstName" value={form.firstName} onChange={handleChange} error={errors.firstName} placeholder="First name" />
                <Field label="Middle Name" name="middleName" value={form.middleName} onChange={handleChange} placeholder="Middle name (optional)" required={false} />
                <Field label="Last Name" name="lastName" value={form.lastName} onChange={handleChange} error={errors.lastName} placeholder="Last name" />

                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-medium text-gray-700">Gender <span className="text-red-500">*</span></label>
                  <select name="gender" value={form.gender} onChange={handleChange}
                    className={`w-full border rounded px-3 py-2.5 text-sm text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-green-400 transition ${errors.gender ? "border-red-400 bg-red-50" : "border-gray-300"}`}>
                    <option value="">Select gender</option>
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                  </select>
                  {errors.gender && <p className="text-xs text-red-500">{errors.gender}</p>}
                </div>

                <Field label="Date of Birth" name="dateOfBirth" type="date" value={form.dateOfBirth} onChange={handleChange} error={errors.dateOfBirth} />
              </div>
            </section>

            {/* Contact */}
            <section>
              <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">Contact Information</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <Field label="Phone Number" name="phoneNumber" type="tel" value={form.phoneNumber} onChange={handleChange} error={errors.phoneNumber} placeholder="+251 9XX XXX XXX" />
                <Field label="Email Address" name="email" type="email" value={form.email} onChange={handleChange} error={errors.email} placeholder="example@email.com" />
              </div>
            </section>

            {/* Account */}
            <section>
              <h2 className="text-xs font-bold text-gray-400 uppercase tracking-widest mb-4">Account & Role</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="flex flex-col gap-1.5">
                  <label className="text-sm font-medium text-gray-700">I am registering as</label>
                  <select name="rolePreference" value={form.rolePreference} onChange={handleChange}
                    className="w-full border border-gray-300 rounded px-3 py-2.5 text-sm text-gray-800 bg-white focus:outline-none focus:ring-2 focus:ring-green-400 transition">
                    <option value="CITIZEN">Citizen</option>
                    <option value="LANDLORD">Landlord</option>
                    <option value="TENANT">Tenant</option>
                  </select>
                </div>
                <div />
                <Field label="Password" name="password" type="password" value={form.password} onChange={handleChange} error={errors.password} placeholder="Min. 8 characters" />
                <Field label="Confirm Password" name="confirmPassword" type="password" value={form.confirmPassword} onChange={handleChange} error={errors.confirmPassword} placeholder="Re-enter password" />
              </div>
            </section>

            <div className="pt-2">
              <button type="submit" disabled={loading}
                className="w-full py-3 bg-gray-900 text-white font-bold text-sm tracking-widest uppercase rounded hover:bg-gray-800 transition-colors disabled:opacity-60 disabled:cursor-not-allowed">
                {loading ? "Registering..." : "Create Account"}
              </button>
              <p className="text-center text-sm text-gray-500 mt-4">
                Already have an account?{" "}
                <Link href="/citizen/login" className="text-green-500 font-semibold hover:underline">Login</Link>
              </p>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

interface FieldProps {
  label: string; name: string; value: string;
  onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  error?: string; placeholder?: string; type?: string; required?: boolean;
}
function Field({ label, name, value, onChange, error, placeholder, type = "text", required = true }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label className="text-sm font-medium text-gray-700">
        {label} {required && <span className="text-red-500">*</span>}
      </label>
      <input type={type} name={name} value={value} onChange={onChange} placeholder={placeholder}
        className={`w-full border rounded px-3 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-green-400 transition ${error ? "border-red-400 bg-red-50" : "border-gray-300 bg-white"}`} />
      {error && <p className="text-xs text-red-500">{error}</p>}
    </div>
  );
}
