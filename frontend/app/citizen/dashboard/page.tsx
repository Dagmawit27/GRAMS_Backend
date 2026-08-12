"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Search, FileText, Building2, CreditCard, UserCircle, PanelLeft } from "lucide-react"
import { getSession, clearSession } from "@/lib/api"
import type { UserSummary } from "@/lib/api"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"

// ── Types ────────────────────────────────────────────────────────────────────

interface LandlordResult {
  id: string
  firstName: string
  lastName: string
  phoneNumber: string
  email: string
}

// ── Mock search (replace with real API call when endpoint is ready) ───────────

async function searchLandlords(query: string): Promise<LandlordResult[]> {
  // TODO: replace with real fetch to /api/v1/users?role=LANDLORD&q=query
  await new Promise((r) => setTimeout(r, 400))
  if (!query.trim()) return []
  return [
    { id: "1", firstName: "Abebe", lastName: "Bekele",  phoneNumber: "+251911000001", email: "abebe@example.com" },
    { id: "2", firstName: "Tigist", lastName: "Haile",  phoneNumber: "+251911000002", email: "tigist@example.com" },
    { id: "3", firstName: "Dawit", lastName: "Tadesse", phoneNumber: "+251911000003", email: "dawit@example.com" },
  ].filter(
    (l) =>
      `${l.firstName} ${l.lastName}`.toLowerCase().includes(query.toLowerCase()) ||
      l.email.toLowerCase().includes(query.toLowerCase())
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function CitizenDashboardPage() {
  const router = useRouter()
  const [user, setUser] = useState<UserSummary | null>(null)

  // landlord search state
  const [query, setQuery] = useState("")
  const [results, setResults] = useState<LandlordResult[]>([])
  const [searching, setSearching] = useState(false)
  const [searched, setSearched] = useState(false)

  useEffect(() => {
    const session = getSession()
    if (!session) { router.push("/citizen/login"); return }
    if (session.user.userType !== "CITIZEN") { router.push("/citizen/login"); return }
    setUser(session.user)
  }, [router])

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    setSearched(false)
    try {
      const data = await searchLandlords(query)
      setResults(data)
    } finally {
      setSearching(false)
      setSearched(true)
    }
  }

  if (!user) {
    return (
      <div className="flex h-screen items-center justify-center text-sm text-muted-foreground">
        Loading...
      </div>
    )
  }

  const initials = `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()

  return (
    <div className="flex flex-col min-h-screen">
      {/* Top bar */}
      <header className="flex h-12 items-center gap-3 border-b px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="h-4" />
        <span className="text-sm font-medium text-muted-foreground">
          Federal Democratic Republic of Ethiopia — GRAMS
        </span>
        <div className="ml-auto flex items-center gap-3">
          <div className="w-7 h-7 rounded-full bg-green-700 flex items-center justify-center text-white text-xs font-bold">
            {initials}
          </div>
          <span className="text-sm font-medium hidden sm:block">
            {user.firstName} {user.lastName}
          </span>
        </div>
      </header>

      <main className="flex-1 p-6 space-y-6 max-w-5xl mx-auto w-full">

        {/* Welcome */}
        <div>
          <h1 className="text-xl font-semibold text-foreground">
            Welcome, {user.firstName} {user.lastName}
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Manage your rental agreements and search for registered landlords.
          </p>
        </div>

        {/* Profile summary cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <Card size="sm">
            <CardHeader>
              <CardDescription>Role</CardDescription>
              <CardTitle>
                <span className="inline-block bg-green-100 text-green-700 text-xs font-semibold px-2.5 py-0.5 rounded-full">
                  {user.roles?.[0] ?? "CITIZEN"}
                </span>
              </CardTitle>
            </CardHeader>
          </Card>
          <Card size="sm">
            <CardHeader>
              <CardDescription>Email</CardDescription>
              <CardTitle className="truncate text-sm font-normal">{user.email}</CardTitle>
            </CardHeader>
          </Card>
          <Card size="sm">
            <CardHeader>
              <CardDescription>Phone</CardDescription>
              <CardTitle className="text-sm font-normal">{user.phoneNumber}</CardTitle>
            </CardHeader>
          </Card>
        </div>

        {/* Landlord Search */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Search className="h-4 w-4 text-green-700" />
              Search Landlords
            </CardTitle>
            <CardDescription>
              Find registered landlords by name or email address.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <form onSubmit={handleSearch} className="flex gap-2">
              <Input
                placeholder="Search by name or email..."
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                className="h-9"
              />
              <Button
                type="submit"
                disabled={searching || !query.trim()}
                className="bg-green-700 hover:bg-green-800 text-white shrink-0"
                size="sm"
              >
                {searching ? "Searching..." : "Search"}
              </Button>
            </form>

            {searched && results.length === 0 && (
              <p className="text-sm text-muted-foreground text-center py-4">
                No landlords found for &quot;{query}&quot;.
              </p>
            )}

            {results.length > 0 && (
              <div className="divide-y divide-border rounded-lg border overflow-hidden">
                {results.map((landlord) => (
                  <div
                    key={landlord.id}
                    className="flex items-center justify-between px-4 py-3 bg-card hover:bg-muted/40 transition-colors"
                  >
                    <div>
                      <p className="text-sm font-medium">
                        {landlord.firstName} {landlord.lastName}
                      </p>
                      <p className="text-xs text-muted-foreground">{landlord.email}</p>
                    </div>
                    <span className="text-xs text-muted-foreground">
                      {landlord.phoneNumber}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Quick action cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <ActionCard icon={<FileText className="h-5 w-5 text-green-700" />} title="Rental Agreements" desc="View, sign, and manage your rental contracts." />
          <ActionCard icon={<Building2 className="h-5 w-5 text-green-700" />} title="Properties" desc="Browse available rental properties in your area." />
          <ActionCard icon={<CreditCard className="h-5 w-5 text-green-700" />} title="Payments" desc="View payment history and upcoming dues." />
          <ActionCard icon={<UserCircle className="h-5 w-5 text-green-700" />} title="My Profile" desc="Update your personal information and credentials." />
        </div>

      </main>
    </div>
  )
}

function ActionCard({
  icon,
  title,
  desc,
}: {
  icon: React.ReactNode
  title: string
  desc: string
}) {
  return (
    <Card className="hover:ring-green-400 transition-all cursor-pointer">
      <CardHeader>
        <div className="flex items-center gap-3">
          {icon}
          <CardTitle>{title}</CardTitle>
        </div>
        <CardDescription>{desc}</CardDescription>
      </CardHeader>
    </Card>
  )
}
