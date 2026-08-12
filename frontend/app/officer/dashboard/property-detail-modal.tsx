"use client"

import { X, Building2, MapPin, FileText, Image } from "lucide-react"
import type { PropertyResponse } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-700",
  VERIFIED: "bg-blue-100 text-blue-700",
  LISTED: "bg-green-100 text-green-700",
  REJECTED: "bg-red-100 text-red-700",
  RENTED: "bg-purple-100 text-purple-700",
  UNLISTED: "bg-gray-100 text-gray-600",
}

export function PropertyDetailModal({
  property: p,
  onClose,
}: {
  property: PropertyResponse
  onClose: () => void
}) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="bg-background rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between p-5 border-b sticky top-0 bg-background z-10">
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-base font-semibold">{p.propertyType}</h2>
              <span className="text-xs font-mono text-muted-foreground">{p.propertyCode}</span>
              <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${STATUS_COLORS[p.status] ?? "bg-gray-100 text-gray-600"}`}>
                {p.status}
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              Submitted {new Date(p.createdAt).toLocaleString()}
            </p>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose} className="h-8 w-8 shrink-0">
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="p-5 space-y-5">
          {/* Property details */}
          <section className="space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground flex items-center gap-1.5">
              <Building2 className="h-3.5 w-3.5" /> Property Details
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-2 text-sm">
              <Detail label="Monthly Rent" value={`ETB ${Number(p.monthlyRent).toLocaleString()}`} />
              {p.bedroomCount != null && <Detail label="Bedrooms" value={String(p.bedroomCount)} />}
              {p.bathroomCount != null && <Detail label="Bathrooms" value={String(p.bathroomCount)} />}
              {p.areaSqMeter != null && <Detail label="Area" value={`${p.areaSqMeter} m²`} />}
              {p.houseNumber && <Detail label="House No." value={p.houseNumber} />}
              {p.floorNumber && <Detail label="Floor" value={p.floorNumber} />}
              {p.furnishingStatus && <Detail label="Furnishing" value={p.furnishingStatus.replace("_", " ")} />}
            </div>
            {p.description && (
              <p className="text-xs text-muted-foreground bg-muted/50 rounded-md px-3 py-2 mt-1">{p.description}</p>
            )}
          </section>

          <Separator />

          {/* Address */}
          <section className="space-y-2">
            <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground flex items-center gap-1.5">
              <MapPin className="h-3.5 w-3.5" /> Address
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-2 text-sm">
              <Detail label="City" value={p.address.city} />
              <Detail label="Sub-city" value={p.address.subCity} />
              <Detail label="Woreda" value={p.address.woreda} />
              {p.address.kebele && <Detail label="Kebele" value={p.address.kebele} />}
              {p.address.street && <Detail label="Street" value={p.address.street} />}
              {p.address.houseNumber && <Detail label="House No." value={p.address.houseNumber} />}
            </div>
          </section>

          {/* Images */}
          {p.images?.length > 0 && (
            <>
              <Separator />
              <section className="space-y-2">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground flex items-center gap-1.5">
                  <Image className="h-3.5 w-3.5" /> Images ({p.images.length})
                </h3>
                <div className="flex flex-wrap gap-2">
                  {p.images.map((img) => (
                    <div key={img.id} className="relative">
                      <div className="w-24 h-20 rounded-md bg-muted flex items-center justify-center text-xs text-muted-foreground overflow-hidden border">
                        {/* eslint-disable-next-line @next/next/no-img-element */}
                        <img src={img.imageUrl} alt="property" className="w-full h-full object-cover" onError={(e) => { (e.target as HTMLImageElement).style.display = "none" }} />
                      </div>
                      {img.isCover && (
                        <span className="absolute top-1 left-1 text-[10px] bg-green-700 text-white px-1 rounded">Cover</span>
                      )}
                    </div>
                  ))}
                </div>
              </section>
            </>
          )}

          {/* Ownership Documents */}
          {p.ownershipDocuments?.length > 0 && (
            <>
              <Separator />
              <section className="space-y-2">
                <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground flex items-center gap-1.5">
                  <FileText className="h-3.5 w-3.5" /> Ownership Documents ({p.ownershipDocuments.length})
                </h3>
                <div className="space-y-1.5">
                  {p.ownershipDocuments.map((doc) => (
                    <div key={doc.id} className="flex items-center justify-between text-xs bg-muted/50 rounded-md px-3 py-2">
                      <div>
                        <span className="font-medium">{doc.documentType}</span>
                        <span className="text-muted-foreground ml-2">#{doc.documentNumber}</span>
                      </div>
                      <div className="text-muted-foreground text-right">
                        {doc.issueDate && <span>Issued: {doc.issueDate}</span>}
                        {doc.expiryDate && <span className="ml-3">Expires: {doc.expiryDate}</span>}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-sm font-medium">{value}</p>
    </div>
  )
}
