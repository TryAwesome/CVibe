'use client';

import Link from "next/link"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { TiltWrapper } from "@/components/ui/tilt-wrapper"
import { useAuth } from "@/lib/contexts/auth-context"
import { Loader2 } from "lucide-react"

export default function RegisterPage() {
  const [name, setName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const { register } = useAuth()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setIsSubmitting(true)

    if (!name || !email || !password) {
      setError("Please fill in all fields")
      setIsSubmitting(false)
      return
    }

    if (password.length < 8) {
      setError("Password must be at least 8 characters")
      setIsSubmitting(false)
      return
    }

    const result = await register(email, password, name)
    
    if (!result.success) {
      setError(result.error || "Registration failed. Please try again.")
    }
    
    setIsSubmitting(false)
  }

  return (
    <TiltWrapper className="w-[480px] max-w-[90vw]">
      <Card className="w-full border-0 bg-black/40 backdrop-blur-md shadow-2xl rounded-3xl text-white">
        <form onSubmit={handleSubmit}>
          <CardHeader className="space-y-1 pb-6">
            <CardTitle className="text-2xl font-bold text-center tracking-tight text-white">
              Create an Account
            </CardTitle>
            <CardDescription className="text-center text-slate-300">
              Join CVibe to start your journey
            </CardDescription>
          </CardHeader>
          <CardContent className="grid gap-6">
            {error && (
              <div className="p-3 rounded-xl bg-red-500/20 border border-red-500/30 text-red-200 text-sm text-center">
                {error}
              </div>
            )}
            <div className="grid gap-2">
              <Label htmlFor="name" className="text-slate-200">Name</Label>
              <Input 
                id="name" 
                placeholder="Your Name" 
                className="bg-white/10 border-white/20 text-white placeholder:text-white/50 focus:border-violet-400 focus:ring-violet-400 rounded-xl"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required 
                disabled={isSubmitting}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="email" className="text-slate-200">Email</Label>
              <Input 
                id="email" 
                type="email" 
                placeholder="m@example.com" 
                className="bg-white/10 border-white/20 text-white placeholder:text-white/50 focus:border-violet-400 focus:ring-violet-400 rounded-xl"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required 
                disabled={isSubmitting}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password" className="text-slate-200">Password</Label>
              <Input 
                id="password" 
                type="password" 
                placeholder="At least 8 characters"
                className="bg-white/10 border-white/20 text-white placeholder:text-white/50 focus:border-violet-400 focus:ring-violet-400 rounded-xl"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required 
                disabled={isSubmitting}
              />
            </div>
            <Button 
              type="submit"
              className="w-full bg-black hover:bg-slate-900 text-white rounded-xl py-6 shadow-lg transition-all hover:scale-[1.02]"
              disabled={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Creating account...
                </>
              ) : (
                'Create Account'
              )}
            </Button>
            
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t border-white/20" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-transparent px-2 text-slate-400 font-medium">
                  or
                </span>
              </div>
            </div>

            <Button 
              type="button"
              variant="outline" 
              className="w-full border-white/20 bg-white/5 hover:bg-white/10 text-white rounded-xl py-6 shadow-sm transition-all hover:scale-[1.02]"
              disabled={isSubmitting}
            >
              <svg className="mr-2 h-4 w-4" aria-hidden="true" focusable="false" data-prefix="fab" data-icon="google" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 488 512">
                <path fill="currentColor" d="M488 261.8C488 403.3 391.1 504 248 504 110.8 504 0 393.2 0 256S110.8 8 248 8c66.8 0 123 24.5 166.3 64.9l-67.5 64.9C258.5 52.6 94.3 116.6 94.3 256c0 86.5 69.1 156.6 153.7 156.6 98.2 0 135-70.4 140.8-106.9H248v-85.3h236.1c2.3 12.7 3.9 24.9 3.9 41.4z"></path>
              </svg>
              Google Register
            </Button>
          </CardContent>
          <CardFooter>
            <div className="w-full text-center text-sm text-slate-300">
              Already have an account?{" "}
              <Link href="/login" className="font-bold text-white hover:underline underline-offset-4">
                Login
              </Link>
            </div>
          </CardFooter>
        </form>
      </Card>
    </TiltWrapper>
  )
}
