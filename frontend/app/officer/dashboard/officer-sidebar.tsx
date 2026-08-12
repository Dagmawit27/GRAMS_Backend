"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { LayoutDashboard, ClipboardCheck, CheckCircle, LogOut } from "lucide-react"
import {
  Sidebar, SidebarContent, SidebarHeader, SidebarFooter,
  SidebarGroup, SidebarGroupLabel, SidebarGroupContent,
  SidebarMenu, SidebarMenuItem, SidebarMenuButton,
} from "@/components/ui/sidebar"
import { clearSession, getSession } from "@/lib/api"
import { useRouter } from "next/navigation"

const navItems = [
  { title: "Dashboard", url: "/officer/dashboard", icon: LayoutDashboard },
  { title: "Review Queue", url: "/officer/dashboard/review", icon: ClipboardCheck },
  { title: "Supervisor Queue", url: "/officer/dashboard/supervisor", icon: CheckCircle },
]

export function OfficerSidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const session = typeof window !== "undefined" ? getSession() : null
  const name = session ? `${session.user.firstName} ${session.user.lastName}` : "Officer"
  const role = session?.user.roles?.[0] ?? ""

  function handleLogout() {
    clearSession()
    router.push("/officer/login")
  }

  return (
    <Sidebar>
      <SidebarHeader className="p-4 border-b">
        <p className="text-xs text-muted-foreground font-medium uppercase tracking-wide">GRAMS</p>
        <h2 className="text-base font-semibold leading-tight">Officer Portal</h2>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupLabel>Navigation</SidebarGroupLabel>
          <SidebarGroupContent>
            <SidebarMenu>
              {navItems.map((item) => (
                <SidebarMenuItem key={item.title}>
                  <SidebarMenuButton
                    render={<Link href={item.url} />}
                    isActive={pathname === item.url}
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

      <SidebarFooter className="p-4 border-t space-y-1">
        <p className="text-xs font-medium truncate">{name}</p>
        <p className="text-xs text-muted-foreground">{role}</p>
        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 text-xs text-red-600 hover:text-red-700 mt-1"
        >
          <LogOut className="h-3.5 w-3.5" /> Sign out
        </button>
      </SidebarFooter>
    </Sidebar>
  )
}
