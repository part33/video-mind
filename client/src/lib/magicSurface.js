import gsap from 'gsap'

const MOBILE_BREAKPOINT = 768

function shouldDisableMagic() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches || window.innerWidth <= MOBILE_BREAKPOINT
}

function setGlowVars(element, x, y, intensity, radius) {
  const rect = element.getBoundingClientRect()
  const relativeX = ((x - rect.left) / rect.width) * 100
  const relativeY = ((y - rect.top) / rect.height) * 100

  element.style.setProperty('--glow-x', `${relativeX}%`)
  element.style.setProperty('--glow-y', `${relativeY}%`)
  element.style.setProperty('--glow-intensity', `${intensity}`)
  element.style.setProperty('--glow-radius', `${radius}px`)
}

export function setupMagicSurface(selector, options = {}) {
  const elements = gsap.utils.toArray(selector)
  if (!elements.length || shouldDisableMagic()) {
    return () => {}
  }

  const {
    glowColor = '201, 168, 255',
    glowRadius = 240,
    tiltAmount = 10,
    magnetism = 0.04,
    clickEffect = true,
  } = options

  const cleanups = elements.map((element) => {
    element.style.setProperty('--glow-color', glowColor)

    const onMouseMove = (event) => {
      const rect = element.getBoundingClientRect()
      const x = event.clientX - rect.left
      const y = event.clientY - rect.top
      const centerX = rect.width / 2
      const centerY = rect.height / 2
      const rotateX = ((y - centerY) / centerY) * -(tiltAmount * 0.6)
      const rotateY = ((x - centerX) / centerX) * tiltAmount
      const offsetX = (x - centerX) * magnetism
      const offsetY = (y - centerY) * magnetism

      setGlowVars(element, event.clientX, event.clientY, 1, glowRadius)

      gsap.to(element, {
        rotateX,
        rotateY,
        x: offsetX,
        y: offsetY,
        duration: 0.35,
        ease: 'power3.out',
        transformPerspective: 1400,
        overwrite: true,
      })
    }

    const onMouseLeave = () => {
      element.style.setProperty('--glow-intensity', '0')
      gsap.to(element, {
        rotateX: 0,
        rotateY: 0,
        x: 0,
        y: 0,
        duration: 0.55,
        ease: 'power3.out',
        overwrite: true,
      })
    }

    const onClick = (event) => {
      if (!clickEffect) {
        return
      }

      const rect = element.getBoundingClientRect()
      const x = event.clientX - rect.left
      const y = event.clientY - rect.top
      const maxDistance = Math.max(
        Math.hypot(x, y),
        Math.hypot(x - rect.width, y),
        Math.hypot(x, y - rect.height),
        Math.hypot(x - rect.width, y - rect.height)
      )

      const ripple = document.createElement('div')
      ripple.className = 'magic-ripple'
      ripple.style.width = `${maxDistance * 2}px`
      ripple.style.height = `${maxDistance * 2}px`
      ripple.style.left = `${x - maxDistance}px`
      ripple.style.top = `${y - maxDistance}px`
      ripple.style.background = `radial-gradient(circle, rgba(${glowColor}, 0.38) 0%, rgba(${glowColor}, 0.18) 34%, transparent 72%)`

      element.appendChild(ripple)

      gsap.fromTo(
        ripple,
        {
          scale: 0,
          opacity: 1,
        },
        {
          scale: 1,
          opacity: 0,
          duration: 0.85,
          ease: 'power2.out',
          onComplete: () => ripple.remove(),
        }
      )
    }

    element.addEventListener('mousemove', onMouseMove)
    element.addEventListener('mouseleave', onMouseLeave)
    element.addEventListener('click', onClick)

    return () => {
      element.removeEventListener('mousemove', onMouseMove)
      element.removeEventListener('mouseleave', onMouseLeave)
      element.removeEventListener('click', onClick)
    }
  })

  return () => {
    cleanups.forEach((cleanup) => cleanup())
  }
}

export function setupSectionSpotlight(sectionSelector, itemSelector, options = {}) {
  const section = document.querySelector(sectionSelector)
  if (!section || shouldDisableMagic()) {
    return () => {}
  }

  const {
    glowColor = '132, 0, 255',
    spotlightRadius = 320,
    maxOpacity = 0.72,
  } = options

  const spotlight = document.createElement('div')
  spotlight.className = 'magic-section-spotlight'
  spotlight.style.background = `radial-gradient(circle, rgba(${glowColor}, 0.16) 0%, rgba(${glowColor}, 0.08) 22%, rgba(${glowColor}, 0.04) 38%, transparent 72%)`
  document.body.appendChild(spotlight)

  const onMouseMove = (event) => {
    const rect = section.getBoundingClientRect()
    const inside =
      event.clientX >= rect.left &&
      event.clientX <= rect.right &&
      event.clientY >= rect.top &&
      event.clientY <= rect.bottom

    const items = Array.from(section.querySelectorAll(itemSelector))

    if (!inside) {
      gsap.to(spotlight, {
        opacity: 0,
        duration: 0.4,
        ease: 'power2.out',
        overwrite: true,
      })
      items.forEach((item) => item.style.setProperty('--glow-intensity', '0'))
      return
    }

    gsap.to(spotlight, {
      left: event.clientX,
      top: event.clientY,
      opacity: maxOpacity,
      duration: 0.2,
      ease: 'power2.out',
      overwrite: true,
    })

    items.forEach((item) => {
      const itemRect = item.getBoundingClientRect()
      const centerX = itemRect.left + itemRect.width / 2
      const centerY = itemRect.top + itemRect.height / 2
      const distance = Math.hypot(event.clientX - centerX, event.clientY - centerY)
      const intensity = Math.max(0, 1 - distance / spotlightRadius)

      setGlowVars(item, event.clientX, event.clientY, intensity, spotlightRadius)
    })
  }

  const onMouseLeave = () => {
    gsap.to(spotlight, {
      opacity: 0,
      duration: 0.4,
      ease: 'power2.out',
      overwrite: true,
    })
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseleave', onMouseLeave)

  return () => {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseleave', onMouseLeave)
    spotlight.remove()
  }
}
