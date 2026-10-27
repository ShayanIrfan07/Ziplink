"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/features/auth/auth-provider";
import { ApiRequestError, errorMessage, loginAccount, registerAccount } from "@/lib/api/client";
import { authSchema, type AuthFormValues } from "@/lib/validation/auth";

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const router = useRouter();
  const { setUser } = useAuth();
  const registering = mode === "register";
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<AuthFormValues>({
    resolver: zodResolver(authSchema),
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit(values: AuthFormValues) {
    try {
      const user = registering ? await registerAccount(values) : await loginAccount(values);
      setUser(user);
      toast.success(registering ? "Account created" : "Signed in");
      router.push("/dashboard");
    } catch (error) {
      if (error instanceof ApiRequestError) {
        for (const field of error.body.fields) {
          if (field.field === "email" || field.field === "password") {
            setError(field.field, { message: field.message });
          }
        }
      }
      toast.error(errorMessage(error));
    }
  }

  return (
    <div className="mx-auto w-full max-w-md px-4 py-16 sm:px-6">
      <p className="text-sm uppercase tracking-[0.16em] text-accent">{registering ? "Create account" : "Welcome back"}</p>
      <h1 className="mt-2 font-display text-4xl tracking-tight">{registering ? "Start your link library" : "Sign in"}</h1>
      <p className="mt-3 text-sm leading-6 text-muted-foreground">
        {registering
          ? "Links already created in this browser move to the new account."
          : "Your dashboard shows only the links that belong to this account."}
      </p>
      <form className="mt-8 space-y-4 rounded-3xl border border-border bg-card p-6" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" autoComplete="email" aria-invalid={errors.email ? true : undefined} {...register("email")} />
          {errors.email ? <p className="text-sm text-danger" role="alert">{errors.email.message}</p> : null}
        </div>
        <div className="space-y-2">
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete={registering ? "new-password" : "current-password"}
            aria-invalid={errors.password ? true : undefined}
            {...register("password")}
          />
          {errors.password ? <p className="text-sm text-danger" role="alert">{errors.password.message}</p> : null}
        </div>
        <Button type="submit" disabled={isSubmitting} className="w-full">
          {isSubmitting ? "Please wait..." : registering ? "Create account" : "Sign in"}
        </Button>
      </form>
      <p className="mt-4 text-sm text-muted-foreground">
        {registering ? "Already have an account?" : "New to Ziplink?"}{" "}
        <Link href={registering ? "/login" : "/register"} className="text-primary underline-offset-4 hover:underline">
          {registering ? "Sign in" : "Create an account"}
        </Link>
      </p>
    </div>
  );
}
