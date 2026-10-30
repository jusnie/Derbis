import React from "react";
import { Download, Smartphone, Zap, ShieldCheck, Music2, ExternalLink } from "lucide-react";
import { isIos } from "../utils/platform";

export default function ApkDownloadBanner() {
  // Hide completely when running on iOS devices or inside the iOS IPA
  if (isIos) return null;
  return (
    <section className="w-full max-w-7xl mx-auto px-4 sm:px-6 py-12">
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-accent/20 via-purple-900/30 to-blue-900/20 border border-white/15 p-8 sm:p-12 shadow-2xl backdrop-blur-3xl">
        
        {/* Glow circles */}
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-accent/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-blue-600/15 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          
          <div className="lg:col-span-8 space-y-4">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-bold bg-white/10 text-white/90 border border-white/10">
              <Smartphone className="w-3.5 h-3.5 text-accent" />
              <span>OFFICIAL ANDROID APP AVAILABLE</span>
            </div>

            <h3 className="text-3xl sm:text-4xl font-black text-white tracking-tight">
              Take Derbis Everywhere on Android
            </h3>

            <p className="text-sm sm:text-base text-white/70 max-w-2xl leading-relaxed">
              Experience lossless offline caching, background playback when your screen is locked, zero advertisements, SponsorBlock skipping, and deep Material You theming on your mobile device.
            </p>

            {/* Feature Pills */}
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2">
              <div className="flex items-center gap-2 p-2.5 rounded-2xl bg-white/[0.04] border border-white/5">
                <Zap className="w-4 h-4 text-amber-400 shrink-0" />
                <span className="text-xs font-semibold text-white/80">Background Play</span>
              </div>
              <div className="flex items-center gap-2 p-2.5 rounded-2xl bg-white/[0.04] border border-white/5">
                <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
                <span className="text-xs font-semibold text-white/80">100% Ad-Free</span>
              </div>
              <div className="flex items-center gap-2 p-2.5 rounded-2xl bg-white/[0.04] border border-white/5">
                <Music2 className="w-4 h-4 text-accent shrink-0" />
                <span className="text-xs font-semibold text-white/80">Continuous Radio</span>
              </div>
            </div>
          </div>

          <div className="lg:col-span-4 flex flex-col items-center lg:items-end justify-center space-y-3">
            <div className="w-full flex flex-col gap-2.5">
              <a
                href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
                download
                className="w-full inline-flex items-center justify-center gap-3 px-6 py-3.5 rounded-2xl bg-gradient-to-r from-accent to-pink-600 hover:from-pink-600 hover:to-accent text-white font-bold text-sm shadow-xl shadow-accent/40 hover:scale-102 active:scale-98 transition-all text-center"
              >
                <Download className="w-4 h-4" />
                <span>Download Android APK</span>
              </a>

              <a
                href="https://drive.google.com/file/d/1hucL-P0hiR2mkccxsT9hzB_Ik9kT4w6K/view?usp=sharing"
                target="_blank"
                rel="noopener noreferrer"
                className="w-full inline-flex items-center justify-center gap-3 px-6 py-3 rounded-2xl bg-gradient-to-r from-cyan-600/30 to-blue-600/30 hover:from-cyan-600/50 hover:to-blue-600/50 text-white font-semibold text-xs border border-cyan-400/30 hover:scale-102 active:scale-98 transition-all text-center"
              >
                <Download className="w-4 h-4 text-cyan-400" />
                <span>Download iOS IPA (iPhone 12 & Newer)</span>
              </a>
            </div>

            <div className="flex flex-wrap items-center justify-center lg:justify-end gap-3 text-xs text-white/50 pt-1">
              <span>APK (~71 MB)</span>
              <span>•</span>
              <span>IPA (iPhone 12+, iOS 16+)</span>
              <span>•</span>
              <a
                href="?install=ios"
                className="text-cyan-400/90 hover:text-cyan-300 font-medium transition-colors underline decoration-cyan-400/40"
                title="Launch Over-The-Air iOS Web Installer"
              >
                1-Tap iOS Web Installer
              </a>
            </div>
          </div>

        </div>

      </div>
    </section>
  );
}
