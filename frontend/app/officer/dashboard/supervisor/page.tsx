"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { CheckCircle, Eye, Ban } from "lucide-react"
import {
  getSession, getPropertiesByStatus, updatePropertyStatus,
  type PropertyResponse,
} from "@/lib/api"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"
import { PropertyDetailModal } from "../property-detail-modal"

export default function SupervisorQueuePage() {
  const router = useRouter()
  const [token, setToken] = useState<string | null>(null)
  const [properties, setProperties] = useState<PropertyResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [selected, setSelected] = useState<PropertyResponse | null>(null)
  const [remarks, setRemarks] = useState("")
  const [acting, setActing] = useState<string | null>(null)

  useEffect(() => {
    const session = getSession()
    if (!session || session.user.userType !== "GOVERNMENT_EMPLOYEE") {
      router.push("/officer/login")
      return
    }
    setToken(session.token)
    load(session.token)
  }, [router])

  async function load(t: string) {
    setLoading(true)
    setError("")
    try {
      const data = await getPropertiesByStatus("VERIFIED", t)
      setProperties(data)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load.")
    } finally {
      setLoading(false)
    }
  }

  async function act(id: string, status: "LISTED" | "REJECTED") {
    if (!token) return
    setActing(id + status)
    try {
      await updatePropertyStatus(token, id, status, remarks || undefined)
      setProperties((prev) => prev.filter((p) => p.id !== id))
      if (selected?.id === id) setSelected(null)
      setRemarks("")
    } catch (e: unknown) {
      alert(e instanceof Error ? e.message : "Action failed.")
    } finally {
      setActing(null)
    }
  }

  return (
    <div className="flex flex-col min-h-screen">
      <header className="flex h-12 items-center gap-3 border-b px-4">
        <SidebarTrigger className="-ml-1" />
        <Separator orientation="vertical" className="h-4" />
        <span className="text-sm font-medium text-muted-foreground">GRAMS — Supervisor Review</span>
      </header>

      <main className="flex-1 p-6 space-y-4 max-w-4xl mx-auto w-full">
        <div>
          <h1 className="text-xl font-semibold flex items-center gap-2">
            <CheckCircle className="h-5 w-5 text-green-700" /> Supervisor Queue
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            VERIFIED properties forwarded by woreda officers. Approve to list publicly, or suspend with remarks.
          </p>
        </div>

        {loading && <p className="text-sm text-muted-foreground py-8 text-center">Loading...</p>}
        {error && <p className="text-sm text-red-600 py-8 text-center">{error}</p>}

        {!loading && !error && properties.length === 0 && (
          <div className="text-center py-12 text-muted-foreground">
            <CheckCircle className="h-10 w-10 mx-auto mb-3 opacity-30" />
            <p className="text-sm">No verified properties awaiting approval.</p>
          </div>
        )}

        {properties.map((p) => (
          <Card key={p.id}>
            <CardHeader className="pb-2">
              <div className="flex items-start justify-between gap-4 flex-wrap">
                <div>
                  <CardTitle className="text-base flex items-center gap-2">
                    {p.propertyType}
                    <span className="text-xs font-mono text-muted-foreground">{p.propertyCode}</span>
                  </CardTitle>
                  <CardDescription>
                    {p.address.subCity}, {p.address.woreda} — {p.address.city}
                  </CardDescription>
                </div>
                <span className="text-xs bg-blue-100 text-blue-700 font-semibold px-2.5 py-0.5 rounded-full">
                  VERIFIED
                </span>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex gap-4 text-xs text-muted-foreground flex-wrap">
                <span>ETB {Number(p.monthlyRent).toLocaleString()}/mo</span>
                {p.bedroomCount != null && <span>{p.bedroomCount} bed</span>}
                {p.bathroomCount != null && <span>{p.bathroomCount} bath</span>}
                {p.areaSqMeter != null && <span>{p.areaSqMeter} m²</span>}
                {p.furnishingStatus && <span>{p.furnishingStatus.replace("_", " ")}</span>}
                <span className="ml-auto">{new Date(p.createdAt).toLocaleDateString()}</span>
              </div>

              {p.description && (
                <p className="text-xs text-muted-foreground line-clamp-2">{p.description}</p>
              )}

              <textarea
                rows={2}
                placeholder="Remarks (optional for approval, required for suspension)"
                value={selected?.id === p.id ? remarks : ""}
                onFocus={() => setSelected(p)}
                onChange={(e) => { setSelected(p); setRemarks(e.target.value) }}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-green-600 resize-none"
              />

              <div className="flex gap-2 flex-wrap">
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setSelected(p)}
                  className="gap-1 text-xs"
                >
                  <Eye className="h-3.5 w-3.5" /> View Details
                </Button>
                <Button
                  size="sm"
                  onClick={() => act(p.id, "LISTED")}
                  disabled={!!acting}
                  className="bg-green-700 hover:bg-green-800 text-white gap-1 text-xs"
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  {acting === p.id + "LISTED" ? "Approving..." : "Approve & List"}
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => act(p.id, "REJECTED")}
                  disabled={!!acting}
                  className="border-red-300 text-red-600 hover:bg-red-50 gap-1 text-xs"
                >
                  <Ban className="h-3.5 w-3.5" />
                  {acting === p.id + "REJECTED" ? "Suspending..." : "Suspend"}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </main>

      {selected && (
        <PropertyDetailModal property={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  )
}
