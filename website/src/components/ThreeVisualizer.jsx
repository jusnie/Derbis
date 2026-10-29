import React, { useEffect, useRef } from "react";
import * as THREE from "three";
import { usePlayer } from "../context/PlayerContext";

export default function ThreeVisualizer({ className = "" }) {
  const mountRef = useRef(null);
  const { isPlaying, visualizerMode, audioFrequencies, currentTrack } = usePlayer();

  const sceneRef = useRef(null);
  const rendererRef = useRef(null);
  const cameraRef = useRef(null);
  const objectsRef = useRef({
    particles: null,
    particleGeo: null,
    vinylGroup: null,
    discMesh: null,
    labelMesh: null,
    wavesMesh: null,
    ambientLight: null,
    pointLight: null
  });
  const animFrameIdRef = useRef(null);

  useEffect(() => {
    const container = mountRef.current;
    if (!container) return;

    const width = container.clientWidth || 400;
    const height = container.clientHeight || 400;

    // Scene
    const scene = new THREE.Scene();
    sceneRef.current = scene;

    // Camera
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    camera.position.set(0, 0, 18);
    cameraRef.current = camera;

    // Renderer with antialias and alpha
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, powerPreference: "high-performance" });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.2;
    rendererRef.current = renderer;

    container.appendChild(renderer.domElement);

    // Lights
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    const pointLight = new THREE.PointLight(0xff2d55, 3, 50);
    pointLight.position.set(5, 5, 10);
    scene.add(pointLight);

    const secondaryLight = new THREE.PointLight(0x00b0ff, 2, 50);
    secondaryLight.position.set(-6, -4, 8);
    scene.add(secondaryLight);

    objectsRef.current.ambientLight = ambientLight;
    objectsRef.current.pointLight = pointLight;

    // ----------------------------------------------------
    // 1. PARTICLES AURA SETUP (Mode: "aura")
    // ----------------------------------------------------
    const particleCount = 2400;
    const particleGeo = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);
    const colors = new Float32Array(particleCount * 3);
    const baseRadii = new Float32Array(particleCount);
    const angles = new Float32Array(particleCount);

    const color1 = new THREE.Color(0xff2d55);
    const color2 = new THREE.Color(0x9c27b0);
    const color3 = new THREE.Color(0x00d2ff);

    for (let i = 0; i < particleCount; i++) {
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(Math.random() * 2 - 1);
      const r = 4.2 + Math.random() * 2.8;

      positions[i * 3] = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i * 3 + 2] = r * Math.cos(phi) * 0.7; // slight oval flattening

      baseRadii[i] = r;
      angles[i] = theta;

      // Gradient color blend
      const mixedColor = color1.clone().lerp(color2, Math.random() * 0.5).lerp(color3, (positions[i * 3 + 2] + 4) / 8);
      colors[i * 3] = mixedColor.r;
      colors[i * 3 + 1] = mixedColor.g;
      colors[i * 3 + 2] = mixedColor.b;
    }

    particleGeo.setAttribute("position", new THREE.BufferAttribute(positions, 3));
    particleGeo.setAttribute("color", new THREE.BufferAttribute(colors, 3));

    // Particle Material
    const particleMat = new THREE.PointsMaterial({
      size: 0.14,
      vertexColors: true,
      transparent: true,
      opacity: 0.85,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });

    const particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);
    objectsRef.current.particles = particles;
    objectsRef.current.particleGeo = particleGeo;

    // ----------------------------------------------------
    // 2. 3D VINYL DISC SETUP (Mode: "vinyl")
    // ----------------------------------------------------
    const vinylGroup = new THREE.Group();
    vinylGroup.position.set(0, 0, 0);
    vinylGroup.rotation.x = 0.35; // gentle isometric tilt

    // Outer vinyl record disc
    const discGeo = new THREE.CylinderGeometry(5.2, 5.2, 0.12, 64);
    const discMat = new THREE.MeshStandardMaterial({
      color: 0x111114,
      roughness: 0.28,
      metalness: 0.85
    });
    const discMesh = new THREE.Mesh(discGeo, discMat);
    discMesh.rotation.x = Math.PI / 2;
    vinylGroup.add(discMesh);

    // Grooves rings (torus lines for tactile sheen)
    for (let r = 2.4; r <= 4.9; r += 0.45) {
      const ringGeo = new THREE.RingGeometry(r, r + 0.02, 64);
      const ringMat = new THREE.MeshBasicMaterial({
        color: 0x22222a,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.6
      });
      const ringMesh = new THREE.Mesh(ringGeo, ringMat);
      ringMesh.position.z = 0.07;
      vinylGroup.add(ringMesh);
    }

    // Center circular label with track artwork texture
    const labelGeo = new THREE.CircleGeometry(2.0, 48);
    const labelTextureLoader = new THREE.TextureLoader();
    const labelTexture = labelTextureLoader.load(
      currentTrack?.thumbnail || "https://i.ytimg.com/vi/g5qU7p7yOY8/hqdefault.jpg"
    );
    labelTexture.generateMipmaps = true;

    const labelMat = new THREE.MeshBasicMaterial({
      map: labelTexture,
      side: THREE.DoubleSide
    });
    const labelMesh = new THREE.Mesh(labelGeo, labelMat);
    labelMesh.position.z = 0.075;
    vinylGroup.add(labelMesh);

    // Center Spindle Hole
    const holeGeo = new THREE.CircleGeometry(0.35, 32);
    const holeMat = new THREE.MeshBasicMaterial({ color: 0x050507, side: THREE.DoubleSide });
    const holeMesh = new THREE.Mesh(holeGeo, holeMat);
    holeMesh.position.z = 0.08;
    vinylGroup.add(holeMesh);

    scene.add(vinylGroup);
    objectsRef.current.vinylGroup = vinylGroup;
    objectsRef.current.discMesh = discMesh;
    objectsRef.current.labelMesh = labelMesh;

    // ----------------------------------------------------
    // 3. SOUND WAVES RIBBON SETUP (Mode: "waves")
    // ----------------------------------------------------
    const waveGeo = new THREE.PlaneGeometry(16, 8, 48, 32);
    const waveMat = new THREE.MeshStandardMaterial({
      color: 0x9c27b0,
      wireframe: true,
      transparent: true,
      opacity: 0.55
    });
    const wavesMesh = new THREE.Mesh(waveGeo, waveMat);
    wavesMesh.rotation.x = -Math.PI / 3;
    wavesMesh.position.y = -2;
    scene.add(wavesMesh);
    objectsRef.current.wavesMesh = wavesMesh;

    // Handle Resize
    const handleResize = () => {
      if (!mountRef.current || !rendererRef.current || !cameraRef.current) return;
      const w = mountRef.current.clientWidth;
      const h = mountRef.current.clientHeight;
      cameraRef.current.aspect = w / h;
      cameraRef.current.updateProjectionMatrix();
      rendererRef.current.setSize(w, h);
    };

    window.addEventListener("resize", handleResize);

    // Mouse movement parallax
    let mouseX = 0;
    let mouseY = 0;
    const handleMouseMove = (e) => {
      const rect = container.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) * 2 - 1;
      const y = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
      mouseX = x * 0.4;
      mouseY = y * 0.4;
    };
    container.addEventListener("mousemove", handleMouseMove);

    // ----------------------------------------------------
    // ANIMATION RENDER LOOP
    // ----------------------------------------------------
    let clock = new THREE.Clock();

    const animate = () => {
      animFrameIdRef.current = requestAnimationFrame(animate);

      const elapsedTime = clock.getElapsedTime();
      const freqs = audioFrequencies || new Float32Array(64);
      const bassVal = freqs[2] || 0.3;
      const midVal = freqs[16] || 0.2;
      const highVal = freqs[32] || 0.1;

      // Parallax camera lerp
      camera.position.x += (mouseX * 2.5 - camera.position.x) * 0.05;
      camera.position.y += (mouseY * 2.5 - camera.position.y) * 0.05;
      camera.lookAt(0, 0, 0);

      // Light pulsing
      if (pointLight) {
        pointLight.intensity = isPlaying ? 2.5 + bassVal * 2.5 : 1.2;
      }

      // Mode visibility control
      const mode = visualizerMode || "aura";
      particles.visible = mode === "aura";
      vinylGroup.visible = mode === "vinyl";
      wavesMesh.visible = mode === "waves";

      // 1. AURA ANIMATION
      if (mode === "aura" && particles) {
        particles.rotation.y = elapsedTime * 0.15;
        particles.rotation.x = Math.sin(elapsedTime * 0.1) * 0.1;

        const posAttr = particleGeo.attributes.position;
        const array = posAttr.array;

        for (let i = 0; i < particleCount; i++) {
          const freqIndex = i % 32;
          const boost = freqs[freqIndex] || 0.1;
          const factor = 1 + boost * (isPlaying ? 0.35 : 0.05);

          const r = baseRadii[i] * factor;
          const phi = Math.acos(positions[i * 3 + 2] / baseRadii[i] || 0.01);
          const theta = angles[i] + elapsedTime * 0.05;

          array[i * 3] = r * Math.sin(phi) * Math.cos(theta);
          array[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
        }
        posAttr.needsUpdate = true;
      }

      // 2. VINYL ANIMATION
      if (mode === "vinyl" && vinylGroup) {
        if (isPlaying) {
          vinylGroup.rotation.z -= 0.025; // 33 RPM spin
        }
        // Audio reactive bounce
        const scaleTarget = 1 + bassVal * 0.06;
        vinylGroup.scale.set(scaleTarget, scaleTarget, scaleTarget);
      }

      // 3. WAVES ANIMATION
      if (mode === "waves" && wavesMesh) {
        const posAttr = waveGeo.attributes.position;
        const array = posAttr.array;
        for (let i = 0; i < posAttr.count; i++) {
          const u = i % 49;
          const v = Math.floor(i / 49);
          const freq = freqs[u % 32] || 0.2;
          const wave = Math.sin(elapsedTime * 2 + u * 0.3) * Math.cos(elapsedTime * 1.5 + v * 0.2);
          array[i * 3 + 2] = wave * (isPlaying ? 1.4 * freq : 0.3);
        }
        posAttr.needsUpdate = true;
      }

      renderer.render(scene, camera);
    };

    animate();

    // Clean up
    return () => {
      if (animFrameIdRef.current) cancelAnimationFrame(animFrameIdRef.current);
      window.removeEventListener("resize", handleResize);
      container.removeEventListener("mousemove", handleMouseMove);

      if (container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }

      particleGeo.dispose();
      particleMat.dispose();
      discGeo.dispose();
      discMat.dispose();
      labelGeo.dispose();
      labelMat.dispose();
      holeGeo.dispose();
      holeMat.dispose();
      waveGeo.dispose();
      waveMat.dispose();
      renderer.dispose();
    };
  }, [visualizerMode, isPlaying]);

  // Update vinyl center label when track thumbnail changes
  useEffect(() => {
    if (objectsRef.current.labelMesh && currentTrack?.thumbnail) {
      new THREE.TextureLoader().load(currentTrack.thumbnail, (tex) => {
        tex.generateMipmaps = true;
        if (objectsRef.current.labelMesh) {
          objectsRef.current.labelMesh.material.map = tex;
          objectsRef.current.labelMesh.material.needsUpdate = true;
        }
      });
    }
  }, [currentTrack]);

  return (
    <div
      ref={mountRef}
      className={`relative w-full h-full overflow-hidden select-none cursor-grab active:cursor-grabbing ${className}`}
      aria-label="Interactive Three.js 3D Audio Visualizer"
    />
  );
}
