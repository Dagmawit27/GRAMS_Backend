import Link from "next/link";

export default function Footer() {
  return (
    <footer className="bg-white border-t border-gray-200 py-6 mt-auto">
      <div className="max-w-6xl mx-auto px-8 flex flex-col md:flex-row items-center justify-between gap-4 text-sm text-gray-500">
        <Link href="/" className="text-green-500 font-semibold tracking-widest uppercase text-xs">
          Rental System
        </Link>
        <p>© {new Date().getFullYear()} Rental System. All rights reserved.</p>
        <div className="flex gap-5">
          <Link href="/login" className="hover:text-green-500 transition-colors">Login</Link>
          <Link href="/register" className="hover:text-green-500 transition-colors">Register</Link>
        </div>
      </div>
    </footer>
  );
}
