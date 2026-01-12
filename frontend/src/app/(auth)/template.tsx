"use client";

import { motion } from "framer-motion";

export default function AuthTemplate({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      transition={{ ease: "easeInOut", duration: 0.4 }}
      className="w-full md:w-auto"
    >
      {children}
    </motion.div>
  );
}
