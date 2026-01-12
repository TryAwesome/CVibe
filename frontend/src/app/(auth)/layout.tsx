export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="relative min-h-screen w-full overflow-hidden">
      {/* Background Image Placeholder - Replace 'bg-slate-900' with valid image path when available */}
      <div 
        className="absolute inset-0 bg-cover bg-center bg-no-repeat z-0"
        style={{ 
          backgroundImage: 'url("/bg.png")',
          filter: 'brightness(0.7)' 
        }} 
      />

      {/* Top Header Line */}
      <div className="relative z-20 flex h-20 w-full items-center justify-center bg-black/50 backdrop-blur-sm border-b border-white/10">
        <h1 className="text-5xl tracking-wider text-white drop-shadow-md font-[family-name:var(--font-zen-dots)]">CVibe</h1>
      </div>

      {/* Main Content Area - Flex container to position card to the right */}
      <div className="relative z-10 flex min-h-[calc(100vh-64px)] w-full items-center justify-center md:justify-end md:pr-24 lg:pr-32">
        {children}
      </div>

      {/* Footer */}
      <div className="absolute bottom-6 w-full text-center z-20">
        <p className="text-white/60 text-sm font-light tracking-widest uppercase">
          Made by Haosen Shi 2026
        </p>
      </div>
    </div>
  );
}
