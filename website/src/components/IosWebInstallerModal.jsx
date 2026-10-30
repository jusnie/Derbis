import React, { useState } from "react";
import { X, Smartphone, Download, Zap, Shield, ExternalLink, Check, Sparkles, HelpCircle, Layers } from "lucide-react";

export default function IosWebInstallerModal({ isOpen, onClose }) {
  const [copiedLink, setCopiedLink] = useState(false);
  const [activeTab, setActiveTab] = useState("ota"); // "ota" | "sidestore" | "pwa"

  if (!isOpen) return null;

  const directIpaUrl = "https://drive.google.com/uc?id=1pbaCauRyCtm-fACHJD7QgLzXEzvVSvWH&export=download";
  const otaManifestUrl = "https://raw.githubusercontent.com/jusnie/Derbis/main/website/public/manifest.plist";
  const itmsLink = `itms-services://?action=download-manifest&url=${encodeURIComponent(otaManifestUrl)}`;

  const handleCopyLink = () => {
    navigator.clipboard.writeText(directIpaUrl);
    setCopiedLink(true);
    setTimeout(() => setCopiedLink(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex items-center justify-center p-4 sm:p-6 select-none animate-fadeIn">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="absolute inset-0 bg-black/85 backdrop-blur-xl transition-opacity"
      />

      {/* Modal Dialog */}
      <div className="relative w-full max-w-lg rounded-3xl bg-gradient-to-b from-[#14141c]/95 to-[#0b0b10]/95 border border-white/15 p-6 sm:p-7 shadow-[0_25px_60px_rgba(0,0,0,0.9)] z-10 space-y-5 text-white">
        
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white shadow-lg shadow-cyan-500/25">
              <Smartphone className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-black tracking-tight text-white flex items-center gap-2">
                <span>Derbis iOS Installer</span>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-cyan-400/20 text-cyan-300 font-bold border border-cyan-400/30">
                  iPhone 12+
                </span>
              </h3>
              <p className="text-xs text-white/50">One-tap installation for iOS devices</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Selection */}
        <div className="grid grid-cols-3 gap-1.5 p-1 rounded-2xl bg-white/[0.04] border border-white/10 text-xs font-bold">
          <button
            onClick={() => setActiveTab("ota")}
            className={`py-2 rounded-xl transition-all ${
              activeTab === "ota"
                ? "bg-white text-black shadow-md"
                : "text-white/60 hover:text-white"
            }`}
          >
            Direct OTA (Web)
          </button>
          <button
            onClick={() => setActiveTab("sidestore")}
            className={`py-2 rounded-xl transition-all ${
              activeTab === "sidestore"
                ? "bg-white text-black shadow-md"
                : "text-white/60 hover:text-white"
            }`}
          >
            SideStore / AltStore
          </button>
          <button
            onClick={() => setActiveTab("pwa")}
            className={`py-2 rounded-xl transition-all ${
              activeTab === "pwa"
                ? "bg-white text-black shadow-md"
                : "text-white/60 hover:text-white"
            }`}
          >
            Safari Standalone
          </button>
        </div>

        {/* Tab Content */}
        {activeTab === "ota" && (
          <div className="space-y-4">
            <div className="p-4 rounded-2xl bg-gradient-to-r from-cyan-950/40 to-blue-950/40 border border-cyan-500/30 space-y-2">
              <div className="flex items-center gap-2 text-cyan-400 text-xs font-bold">
                <Sparkles className="w-4 h-4" />
                <span>Apple itms-services:// Wireless Installer</span>
              </div>
              <p className="text-xs text-white/70 leading-relaxed">
                Tapping the button below prompts iOS to fetch the manifest and install Derbis directly onto your home screen wirelessly.
              </p>
            </div>

            <a
              href={itmsLink}
              className="w-full flex items-center justify-center gap-3 py-3.5 rounded-2xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-extrabold text-sm shadow-xl shadow-cyan-500/30 active:scale-98 transition-all text-center"
            >
              <Download className="w-4 h-4" />
              <span>Tap to Install on iPhone (Safari)</span>
            </a>

            <p className="text-[11px] text-white/40 text-center leading-normal">
              Note: Apple's native OTA prompt requires an ad-hoc or enterprise certificate. If your iOS warns "Integrity could not be verified", use the SideStore or Sideloadly tab.
            </p>
          </div>
        )}

        {activeTab === "sidestore" && (
          <div className="space-y-3">
            <p className="text-xs text-white/70 leading-relaxed">
              Open directly in your favorite on-device iOS sideloading app with zero computer needed:
            </p>

            <div className="grid grid-cols-2 gap-2.5">
              <a
                href={`sidestore://install?url=${encodeURIComponent(directIpaUrl)}`}
                className="flex items-center justify-between p-3 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-bold transition-all text-white"
              >
                <span>SideStore 1-Click</span>
                <Zap className="w-3.5 h-3.5 text-cyan-400" />
              </a>

              <a
                href={`altstore://install?url=${encodeURIComponent(directIpaUrl)}`}
                className="flex items-center justify-between p-3 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-bold transition-all text-white"
              >
                <span>AltStore 1-Click</span>
                <Zap className="w-3.5 h-3.5 text-emerald-400" />
              </a>

              <a
                href={`apple-magnifier://install?url=${encodeURIComponent(directIpaUrl)}`}
                className="flex items-center justify-between p-3 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-bold transition-all text-white"
              >
                <span>TrollStore 1-Click</span>
                <Shield className="w-3.5 h-3.5 text-amber-400" />
              </a>

              <a
                href={`sideloadly://install?url=${encodeURIComponent(directIpaUrl)}`}
                className="flex items-center justify-between p-3 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-xs font-bold transition-all text-white"
              >
                <span>Sideloadly Desktop</span>
                <Download className="w-3.5 h-3.5 text-purple-400" />
              </a>
            </div>

            <button
              onClick={handleCopyLink}
              className="w-full py-2.5 rounded-xl bg-white/10 hover:bg-white/15 text-white/80 hover:text-white text-xs font-semibold flex items-center justify-center gap-2 border border-white/10 transition-colors"
            >
              {copiedLink ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Layers className="w-3.5 h-3.5" />}
              <span>{copiedLink ? "IPA Link Copied to Clipboard!" : "Copy Direct .IPA Download Link"}</span>
            </button>
          </div>
        )}

        {activeTab === "pwa" && (
          <div className="space-y-3.5 p-4 rounded-2xl bg-[#0f0f15] border border-white/10">
            <div className="flex items-center gap-2 text-amber-400 text-xs font-bold">
              <Zap className="w-4 h-4" />
              <span>Instant Home Screen App (0 Certificates Needed)</span>
            </div>
            
            <ol className="text-xs text-white/70 space-y-2 list-decimal list-inside leading-relaxed">
              <li>Open this website in <strong>Safari</strong> on your iPhone.</li>
              <li>Tap the <strong>Share button</strong> (square with arrow up) at the bottom.</li>
              <li>Scroll down and tap <strong>"Add to Home Screen"</strong>.</li>
              <li>Tap <strong>Add</strong> at top right.</li>
            </ol>

            <p className="text-[11px] text-emerald-400/90 font-medium">
              ✓ Fullscreen edge-to-edge layout • Lock Screen controls • Never expires after 7 days
            </p>
          </div>
        )}

        {/* Open Source Tools References */}
        <div className="pt-2 border-t border-white/10 flex items-center justify-between text-[11px] text-white/40">
          <span>Explore GitHub OTA Repos:</span>
          <div className="flex items-center gap-3">
            <a
              href="https://github.com/Real3rr0r303YT/Websigner-iOS"
              target="_blank"
              rel="noreferrer"
              className="hover:text-cyan-400 transition-colors flex items-center gap-0.5"
            >
              <span>WebSigner</span>
              <ExternalLink className="w-2.5 h-2.5" />
            </a>
            <span>•</span>
            <a
              href="https://github.com/mohith-labs/ipa-installer"
              target="_blank"
              rel="noreferrer"
              className="hover:text-cyan-400 transition-colors flex items-center gap-0.5"
            >
              <span>ipa-installer</span>
              <ExternalLink className="w-2.5 h-2.5" />
            </a>
            <span>•</span>
            <a
              href="https://github.com/SideStore/SideStore"
              target="_blank"
              rel="noreferrer"
              className="hover:text-cyan-400 transition-colors flex items-center gap-0.5"
            >
              <span>SideStore</span>
              <ExternalLink className="w-2.5 h-2.5" />
            </a>
          </div>
        </div>

      </div>
    </div>
  );
}
