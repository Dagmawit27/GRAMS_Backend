import Link from "next/link";

export default function UnauthorizedPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full text-center">
        <h1 className="text-3xl font-bold text-red-600 mb-2">403 Access Denied</h1>
        <p className="text-gray-600 mb-6">
          You do not have authorization to access this portal URL. Government and Administrator portals are strictly restricted.
        </p>
        <Link
          href="/login"
          className="inline-block bg-gray-900 text-white font-bold py-2.5 px-6 rounded hover:bg-gray-800 transition"
        >
          Return to Login
        </Link>
      </div>
    </div>
  );
}
