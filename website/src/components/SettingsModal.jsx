import React from "react";
import { X, Smartphone, ExternalLink, ShieldCheck, Zap, Download } from "lucide-react";

export default function SettingsModal({ isOpen, onClose }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="absolute inset-0 bg-black/75 backdrop-blur-md transition-opacity"
      />

      {/* Dialog */}
      <div className="relative w-full max-w-lg rounded-3xl bg-[#111216] border border-white/15 p-6 shadow-2xl z-10 space-y-5">
        <div className="flex items-center justify-between border-b border-white/10 pb-4">
          <div className="flex items-center gap-2.5">
            <img src="/assets/derbis-logo.png" alt="Derbis" className="w-6 h-6 object-contain" />
            <h3 className="text-base font-black text-white">Settings & Account</h3>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-white/50 hover:text-white hover:bg-white/10"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Account Info */}
        <div className="flex items-center gap-3 p-3.5 rounded-2xl bg-white/[0.04] border border-white/10">
          <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-pink-500 to-rose-700 flex items-center justify-center text-white text-sm font-black shrink-0">
            Y
          </div>
          <div className="min-w-0 flex-1">
            <strong className="block text-sm font-bold text-white">Yasuda</strong>
            <small className="block text-xs text-white/50">Derbis Web Companion Session</small>
          </div>
          <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-blue-500/20 text-blue-400 border border-blue-500/30">
            Web v1.2.0
          </span>
        </div>

        {/* Android App Promotion */}
        <div className="p-4 rounded-2xl bg-gradient-to-r from-blue-900/30 to-purple-900/30 border border-blue-500/30 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-blue-400" />
              <strong className="text-xs font-bold text-white">Derbis for Android</strong>
            </div>
            <span className="text-[10px] font-bold text-white/50">v1.1.1</span>
          </div>
          <p className="text-xs text-white/70 leading-relaxed">
            Download the native APK to unlock offline caching, background playback with screen locked, and zero ads.
          </p>
          <a
            href="https://github.com/jusnie/Derbis/releases/latest/download/Derbis.apk"
            download
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold transition-all shadow"
          >
            <Download className="w-4 h-4" />
            <span>Download APK (~71 MB)</span>
          </a>
        </div>

        {/* GitHub link */}
        <div className="flex items-center justify-between pt-2 text-xs text-white/50 border-t border-white/10">
          <span>Open Source Project</span>
          <a
            href="https://github.com/jusnie/Derbis"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1 text-blue-400 hover:underline"
          >
            <span>GitHub Repository</span>
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </div>
    </div>
  );
}
