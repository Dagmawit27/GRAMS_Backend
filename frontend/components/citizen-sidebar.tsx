"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  LayoutDashboard,
  FileText,
  Building2,
  CreditCard,
  UserCircle,
  Search,
  LogOut,
} from "lucide-react"
import {
  Sidebar,
  SidebarContent,
  SidebarHeader,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarGroupContent,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
} from "@/components/ui/sidebar"
import { Separator } from "@/components/ui/separator"

const navItems = [
  { title: "Dashboard",         url: "/citizen/dashboard",           icon: LayoutDashboard },
  { title: "Search House",  url: "/citizen/dashboard?tab=search", icon: Search },
  { title: "Rental Agreements", url: "/citizen/dashboard?tab=agreements", icon: FileText },
  { title: "Properties",        url: "/citizen/dashboard/properties", icon: Building2 },
  { title: "Payments",          url: "/citizen/dashboard?tab=payments", icon: CreditCard },
  { title: "My Profile",        url: "/citizen/dashboard?tab=profile", icon: UserCircle },
]

interface CitizenSidebarProps {
  userName?: string
  onLogout?: () => void
}

export function CitizenSidebar({ userName, onLogout }: CitizenSidebarProps) {
  const pathname = usePathname()

  return (
    <Sidebar>
      <SidebarHeader className="px-4 py-5">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-green-700 flex items-center justify-center shrink-0">
            <span className="text-white text-base font-bold">★</span>
          </div>
          <div>
            <p className="text-sm font-bold leading-tight">GRAMS</p>
            <p className="text-[11px] text-muted-foreground leading-tight">
              Citizen Portal
            </p>
          </div>
        </div>
        <div className="mt-3 h-0.5 w-full rounded-full bg-gradient-to-r from-green-600 via-yellow-400 to-red-600" />
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navItems.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    isActive={pathname === item.url.split("?")[0]}
                    render={<Link href={item.url} />}
                  >
                    <item.icon className="h-4 w-4" />
                    <span>{item.title}</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarFooter className="px-4 py-4">
        <Separator className="mb-3" />
        <div className="flex items-center justify-between">
          <span className="text-xs text-muted-foreground truncate max-w-[130px]">
            {userName ?? "Citizen"}
          </span>
          {onLogout && (
            <button
              onClick={onLogout}
              className="flex items-center gap-1 text-xs text-red-500 hover:text-red-600 transition-colors"
            >
              <LogOut className="h-3.5 w-3.5" />
              Logout
            </button>
          )}
        </div>
        <p className="text-[10px] text-muted-foreground mt-2 leading-tight">
          Ministry of Urban Development<br />Addis Ababa, Ethiopia
        </p>
      </SidebarFooter>
    </Sidebar>
  )
}
