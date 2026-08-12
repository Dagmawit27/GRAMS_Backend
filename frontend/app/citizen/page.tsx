import Link from "next/link";

export default function CitizenHome() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-50 px-4">
      {/* Header / Branding */}
      <div className="flex flex-col items-center gap-2 mb-10">
        <div className="w-16 h-16 rounded-full bg-green-700 flex items-center justify-center mb-2">
          {/* Ethiopian star emblem placeholder */}
          <span className="text-white text-3xl font-bold">★</span>
        </div>
        <h1 className="text-2xl font-bold text-gray-900 tracking-wide text-center">
          Federal Democratic Republic of Ethiopia
        </h1>
        <p className="text-sm text-gray-500 tracking-widest uppercase">
          Digital Rental Agreement System
        </p>
        <div className="w-24 h-1 mt-2 rounded-full bg-gradient-to-r from-green-600 via-yellow-400 to-red-600" />
      </div>

      {/* Main Card */}
      <div className="bg-white rounded-2xl shadow-md w-full max-w-md px-8 py-10 flex flex-col items-center gap-6 border border-gray-100">
        <div className="text-center">
          <h2 className="text-xl font-semibold text-gray-800 mb-2">
            Citizen Portal
          </h2>
          <p className="text-sm text-gray-500 leading-relaxed">
            Welcome to the official citizen access portal. Register to submit
            and manage your rental agreements, or log in to your existing
            account.
          </p>
        </div>

        <div className="flex flex-col w-full gap-3 mt-2">
          <Link
            href="/citizen/register"
            className="w-full text-center bg-green-700 hover:bg-green-800 text-white font-semibold py-3 rounded-lg transition-colors"
          >
            Register Now
          </Link>
          <Link
            href="/citizen/login"
            className="w-full text-center border border-gray-300 hover:bg-gray-50 text-gray-800 font-semibold py-3 rounded-lg transition-colors"
          >
            Log In
          </Link>
        </div>
      </div>

      {/* Footer note */}
      <p className="mt-8 text-xs text-gray-400 text-center">
        Ministry of Urban Development &amp; Construction &nbsp;|&nbsp; Addis
        Ababa, Ethiopia
      </p>
    </div>
  );
}
