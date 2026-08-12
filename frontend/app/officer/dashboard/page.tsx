"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { ClipboardCheck, CheckCircle, Building2 } from "lucide-react"
import { getSession } from "@/lib/api"
import { Card, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"
import Link from "next/link"
import type { UserSummary } from "@/lib/api"

export default function OfficerDashboardPage() {
  const router = useRouter()
  const [user, setUser] = useState<UserSummary | null>(null)

  useEffect(() => {
    const session = getSession()
    if (!session || session.user.userType !== "GOVERNMENT_EMPLOYEE") {
      router.push("/officer/login")
      return
    }
    setUser(session.user)
  }, [router])

  if (!user) return (
    <div className="flex h-screen items-center justify-center text-sm text-muted-foreground">Loading...</div>
  )

  const initials = `${user.firstName[0]}${user.lastName[0]}`.toUpperCase()
  const role = user.roles?.[0] ?? ""
  const isOfficer = role === "WOREDA_OFFICER"
  const isSupervisor = role === "WOREDA_SUPERVISOR"

  return (
    <div className="flex flex-col min-h-screen">
      <header className="flex h-12 items-center gap-3 border-b px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="h-4" />
        <span className="text-sm font-medium text-muted-foreground">
          Federal Democratic Republic of Ethiopia — GRAMS
        </span>
        <div className="ml-auto flex items-center gap-3">
          <div className="w-7 h-7 rounded-full bg-blue-700 flex items-center justify-center text-white text-xs font-bold">
            {initials}
          </div>
          <span className="text-sm font-medium hidden sm:block">{user.firstName} {user.lastName}</span>
        </div>
      </header>

      <main className="flex-1 p-6 space-y-6 max-w-4xl mx-auto w-full">
        <div>
          <h1 className="text-xl font-semibold">Welcome, {user.firstName}</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {role.replace(/_/g, " ")} — Government Rental & Asset Management System
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {(isOfficer || !isSupervisor) && (
            <Link href="/officer/dashboard/review">
              <Card className="hover:ring-1 hover:ring-blue-400 transition-all cursor-pointer">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <ClipboardCheck className="h-5 w-5 text-blue-700" />
                    <CardTitle>Review Queue</CardTitle>
                  </div>
                  <CardDescription>
                    Review PENDING properties submitted by landlords. Forward verified ones to the supervisor.
                  </CardDescription>
                </CardHeader>
              </Card>
            </Link>
          )}

          {(isSupervisor || !isOfficer) && (
            <Link href="/officer/dashboard/supervisor">
              <Card className="hover:ring-1 hover:ring-green-400 transition-all cursor-pointer">
                <CardHeader>
                  <div className="flex items-center gap-3">
                    <CheckCircle className="h-5 w-5 text-green-700" />
                    <CardTitle>Supervisor Queue</CardTitle>
                  </div>
                  <CardDescription>
                    Approve or suspend VERIFIED properties. Approved ones become publicly LISTED.
                  </CardDescription>
                </CardHeader>
              </Card>
            </Link>
          )}

          <Card>
            <CardHeader>
              <div className="flex items-center gap-3">
                <Building2 className="h-5 w-5 text-muted-foreground" />
                <CardTitle>Listed Properties</CardTitle>
              </div>
              <CardDescription>Browse all currently active listed rental properties.</CardDescription>
            </CardHeader>
          </Card>
        </div>
      </main>
    </div>
  )
}
