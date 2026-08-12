"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Building2, PlusCircle, X } from "lucide-react"
import {
  getSession,
  registerProperty,
  getMyProperties,
  type PropertyResponse,
  type PropertyRequest,
} from "@/lib/api"
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { SidebarTrigger } from "@/components/ui/sidebar"

const PROPERTY_TYPES = ["APARTMENT", "HOUSE", "VILLA", "STUDIO", "OFFICE", "SHOP"]
const FURNISHING_OPTIONS = ["FURNISHED", "SEMI_FURNISHED", "UNFURNISHED"]

function StatusBadge({ status }: { status: string }) {
  const colors: Record<string, string> = {
    PENDING: "bg-yellow-100 text-yellow-700",
    VERIFIED: "bg-blue-100 text-blue-700",
    LISTED: "bg-green-100 text-green-700",
    REJECTED: "bg-red-100 text-red-700",
    RENTED: "bg-purple-100 text-purple-700",
    UNLISTED: "bg-gray-100 text-gray-600",
  }
  return (
    <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${colors[status] ?? "bg-gray-100 text-gray-600"}`}>
      {status}
    </span>
  )
}

export default function PropertiesPage() {
  const router = useRouter()
  const [token, setToken] = useState<string | null>(null)
  const [userName, setUserName] = useState("")

  // list state
  const [properties, setProperties] = useState<PropertyResponse[]>([])
  const [loadingList, setLoadingList] = useState(true)
  const [listError, setListError] = useState("")

  // form visibility
  const [showForm, setShowForm] = useState(false)

  // form state
  const [form, setForm] = useState<PropertyRequest>({
    propertyType: "",
    address: { city: "", subCity: "", woreda: "" },
    monthlyRent: 0,
  })
  const [images, setImages] = useState<File[]>([])
  const [documents, setDocuments] = useState<File[]>([])
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState("")
  const [submitSuccess, setSubmitSuccess] = useState<PropertyResponse | null>(null)

  useEffect(() => {
    const session = getSession()
    if (!session) { router.push("/citizen/login"); return }
    setToken(session.token)
    setUserName(`${session.user.firstName} ${session.user.lastName}`)
    loadProperties(session.token)
  }, [router])

  async function loadProperties(t: string) {
    setLoadingList(true)
    setListError("")
    try {
      const data = await getMyProperties(t)
      setProperties(data)
    } catch (e: unknown) {
      setListError(e instanceof Error ? e.message : "Failed to load properties.")
    } finally {
      setLoadingList(false)
    }
  }

  function setField<K extends keyof PropertyRequest>(key: K, val: PropertyRequest[K]) {
    setForm((f) => ({ ...f, [key]: val }))
  }

  function setAddressField(key: string, val: string) {
    setForm((f) => ({ ...f, address: { ...f.address, [key]: val } }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!token) return
    setSubmitting(true)
    setSubmitError("")
    setSubmitSuccess(null)
    try {
      const result = await registerProperty(token, form, images, documents)
      setSubmitSuccess(result)
      setProperties((prev) => [result, ...prev])
      setShowForm(false)
      setForm({ propertyType: "", address: { city: "", subCity: "", woreda: "" }, monthlyRent: 0 })
      setImages([])
      setDocuments([])
    } catch (e: unknown) {
      setSubmitError(e instanceof Error ? e.message : "Something went wrong.")
    } finally {
      setSubmitting(false)
    }
  }

  const initials = userName.split(" ").map((n) => n[0]).join("").toUpperCase().slice(0, 2)

  return (
    <div className="flex flex-col min-h-screen">
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
          <span className="text-sm font-medium hidden sm:block">{userName}</span>
        </div>
      </header>

      <main className="flex-1 p-6 space-y-6 max-w-4xl mx-auto w-full">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-semibold">My Properties</h1>
            <p className="text-sm text-muted-foreground mt-0.5">Register and manage your rental properties.</p>
          </div>
          <Button
            onClick={() => { setShowForm((v) => !v); setSubmitError(""); setSubmitSuccess(null) }}
            className="bg-green-700 hover:bg-green-800 text-white gap-2"
            size="sm"
          >
            {showForm ? <X className="h-4 w-4" /> : <PlusCircle className="h-4 w-4" />}
            {showForm ? "Cancel" : "Register Property"}
          </Button>
        </div>

        {/* Success banner */}
        {submitSuccess && (
          <div className="rounded-lg border border-green-300 bg-green-50 px-4 py-3 text-sm text-green-800">
            Property registered successfully — code: <strong>{submitSuccess.propertyCode}</strong>. Status: <StatusBadge status={submitSuccess.status} />
          </div>
        )}

        {/* Registration Form */}
        {showForm && (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-base">
                <Building2 className="h-4 w-4 text-green-700" />
                Register New Property
              </CardTitle>
              <CardDescription>Fill in the details below. Required fields are marked *.</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleSubmit} className="space-y-5">
                {/* Property basics */}
                <fieldset className="space-y-3">
                  <legend className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Property Details</legend>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Property Type *</label>
                      <select
                        required
                        value={form.propertyType}
                        onChange={(e) => setField("propertyType", e.target.value)}
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                      >
                        <option value="">Select type</option>
                        {PROPERTY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Monthly Rent (ETB) *</label>
                      <Input
                        type="number"
                        min="0"
                        required
                        placeholder="e.g. 5000"
                        value={form.monthlyRent || ""}
                        onChange={(e) => setField("monthlyRent", parseFloat(e.target.value) || 0)}
                        className="h-9"
                      />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Bedrooms</label>
                      <Input type="number" min="0" placeholder="0" value={form.bedroomCount ?? ""} onChange={(e) => setField("bedroomCount", parseInt(e.target.value) || undefined)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Bathrooms</label>
                      <Input type="number" min="0" placeholder="0" value={form.bathroomCount ?? ""} onChange={(e) => setField("bathroomCount", parseInt(e.target.value) || undefined)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Area (m²)</label>
                      <Input type="number" min="0" step="0.1" placeholder="e.g. 80" value={form.areaSqMeter ?? ""} onChange={(e) => setField("areaSqMeter", parseFloat(e.target.value) || undefined)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Furnishing Status</label>
                      <select
                        value={form.furnishingStatus ?? ""}
                        onChange={(e) => setField("furnishingStatus", e.target.value || undefined)}
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm focus:outline-none focus:ring-2 focus:ring-green-600"
                      >
                        <option value="">Select</option>
                        {FURNISHING_OPTIONS.map((o) => <option key={o} value={o}>{o.replace("_", " ")}</option>)}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">House Number</label>
                      <Input placeholder="e.g. 42B" value={form.houseNumber ?? ""} onChange={(e) => setField("houseNumber", e.target.value || undefined)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Floor Number</label>
                      <Input placeholder="e.g. 3" value={form.floorNumber ?? ""} onChange={(e) => setField("floorNumber", e.target.value || undefined)} className="h-9" />
                    </div>
                  </div>
                  <div className="space-y-1">
                    <label className="text-xs font-medium">Description</label>
                    <textarea
                      rows={3}
                      placeholder="Brief description of the property..."
                      value={form.description ?? ""}
                      onChange={(e) => setField("description", e.target.value || undefined)}
                      className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-green-600 resize-none"
                    />
                  </div>
                </fieldset>

                <Separator />

                {/* Address */}
                <fieldset className="space-y-3">
                  <legend className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Address</legend>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                    <div className="space-y-1">
                      <label className="text-xs font-medium">City *</label>
                      <Input required placeholder="Addis Ababa" value={form.address.city} onChange={(e) => setAddressField("city", e.target.value)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Sub-city *</label>
                      <Input required placeholder="Bole" value={form.address.subCity} onChange={(e) => setAddressField("subCity", e.target.value)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Woreda *</label>
                      <Input required placeholder="03" value={form.address.woreda} onChange={(e) => setAddressField("woreda", e.target.value)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Kebele</label>
                      <Input placeholder="01" value={form.address.kebele ?? ""} onChange={(e) => setAddressField("kebele", e.target.value)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Street</label>
                      <Input placeholder="Street name" value={form.address.street ?? ""} onChange={(e) => setAddressField("street", e.target.value)} className="h-9" />
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">House No.</label>
                      <Input placeholder="e.g. 42" value={form.address.houseNumber ?? ""} onChange={(e) => setAddressField("houseNumber", e.target.value)} className="h-9" />
                    </div>
                  </div>
                </fieldset>

                <Separator />

                {/* File uploads */}
                <fieldset className="space-y-3">
                  <legend className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Attachments</legend>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Property Images</label>
                      <input
                        type="file"
                        multiple
                        accept="image/*"
                        onChange={(e) => setImages(Array.from(e.target.files ?? []))}
                        className="w-full text-sm text-muted-foreground file:mr-3 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100"
                      />
                      {images.length > 0 && <p className="text-xs text-muted-foreground">{images.length} file(s) selected</p>}
                    </div>
                    <div className="space-y-1">
                      <label className="text-xs font-medium">Ownership Documents</label>
                      <input
                        type="file"
                        multiple
                        accept=".pdf,.jpg,.jpeg,.png"
                        onChange={(e) => setDocuments(Array.from(e.target.files ?? []))}
                        className="w-full text-sm text-muted-foreground file:mr-3 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-green-50 file:text-green-700 hover:file:bg-green-100"
                      />
                      {documents.length > 0 && <p className="text-xs text-muted-foreground">{documents.length} file(s) selected</p>}
                    </div>
                  </div>
                </fieldset>

                {submitError && (
                  <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-md px-3 py-2">{submitError}</p>
                )}

                <div className="flex justify-end gap-2 pt-1">
                  <Button type="button" variant="outline" size="sm" onClick={() => setShowForm(false)}>Cancel</Button>
                  <Button type="submit" disabled={submitting} className="bg-green-700 hover:bg-green-800 text-white" size="sm">
                    {submitting ? "Submitting..." : "Register Property"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        )}

        {/* Properties list */}
        {loadingList ? (
          <p className="text-sm text-muted-foreground text-center py-8">Loading properties...</p>
        ) : listError ? (
          <p className="text-sm text-red-600 text-center py-8">{listError}</p>
        ) : properties.length === 0 ? (
          <div className="text-center py-12 text-muted-foreground">
            <Building2 className="h-10 w-10 mx-auto mb-3 opacity-30" />
            <p className="text-sm">No properties registered yet.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {properties.map((p) => (
              <Card key={p.id} className="hover:shadow-sm transition-shadow">
                <CardContent className="pt-4 pb-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-medium text-sm">{p.propertyType}</span>
                        <span className="text-xs text-muted-foreground font-mono">{p.propertyCode}</span>
                        <StatusBadge status={p.status} />
                      </div>
                      <p className="text-xs text-muted-foreground">
                        {p.address.subCity}, {p.address.woreda} — {p.address.city}
                      </p>
                      <div className="flex gap-4 text-xs text-muted-foreground pt-0.5 flex-wrap">
                        <span>ETB {p.monthlyRent.toLocaleString()}/mo</span>
                        {p.bedroomCount != null && <span>{p.bedroomCount} bed</span>}
                        {p.bathroomCount != null && <span>{p.bathroomCount} bath</span>}
                        {p.areaSqMeter != null && <span>{p.areaSqMeter} m²</span>}
                        {p.furnishingStatus && <span>{p.furnishingStatus.replace("_", " ")}</span>}
                      </div>
                    </div>
                    <div className="text-xs text-muted-foreground shrink-0">
                      {new Date(p.createdAt).toLocaleDateString()}
                    </div>
                  </div>
                  {p.description && (
                    <p className="text-xs text-muted-foreground mt-2 line-clamp-2">{p.description}</p>
                  )}
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </main>
    </div>
  )
}
