import { z } from "zod";

export const authSchema = z.object({
  email: z.string().trim().email("Enter a valid email").max(254, "Email must be at most 254 characters"),
  password: z.string().min(8, "Password must be 8 to 72 characters").max(72, "Password must be 8 to 72 characters"),
});

export type AuthFormValues = z.infer<typeof authSchema>;
