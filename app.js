document.addEventListener('DOMContentLoaded', () => {
  const themeButtons = document.querySelectorAll('.theme-btn');
  const body = document.body;

  // App UI Mockup Elements to update dynamically
  const mockGreeting = document.querySelector('.mock-greeting');
  const mockQuote = document.querySelector('.mock-quote');
  const mockShieldIcon = document.querySelector('.shield-icon');
  const mockShieldState = document.querySelector('.shield-state');
  const mockFocusStatus = document.querySelector('.mock-focus-status');
  const mockFocusSubtext = document.querySelector('.mock-focus-subtext');
  const mockActionBtn = document.querySelector('.mock-action-btn');

  // Simulated state tracking
  let isFocusActive = false;

  // Quotes mapping to each theme for immersive experience
  const themeDetails = {
    'aether-neon': {
      class: 'theme-aether-neon',
      greeting: 'Good afternoon, Advaith 🌤️',
      quote: '"Your phone can wait. Your dreams can\'t."',
    },
    'cyberpunk': {
      class: 'theme-cyberpunk',
      greeting: 'Night owl mode, Advaith 🦉',
      quote: '"Deep work is the superpower of the 21st century."',
    },
    'forest-oasis': {
      class: 'theme-forest-oasis',
      greeting: 'Good morning, Advaith ☀️',
      quote: '"Silence is the sleep that nourishes wisdom."',
    },
    'obsidian': {
      class: 'theme-obsidian',
      greeting: 'Good evening, Advaith 🌅',
      quote: '"Focus is not about saying yes. It\'s about saying no."',
    },
    'snow-drift': {
      class: 'theme-snow-drift',
      greeting: 'Burning the midnight oil? 🌙',
      quote: '"Every minute of focus compounds over time."',
    }
  };

  themeButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const selectedThemeKey = btn.getAttribute('data-theme');
      const data = themeDetails[selectedThemeKey];

      if (!data) return;

      // 1. Remove all other theme classes
      Object.values(themeDetails).forEach(info => {
        body.classList.remove(info.class);
      });

      // 2. Add selected theme class
      body.classList.add(data.class);

      // 3. Update active button state
      themeButtons.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');

      // 4. Update Mock Phone UI text details to simulate live app
      mockGreeting.textContent = data.greeting;
      mockQuote.textContent = data.quote;

      // Reset mock focus state on theme switch
      resetMockFocusState();
    });
  });

  // Mock central button interaction
  const shieldBtn = document.querySelector('.mock-shield-button');
  const actionBtn = document.querySelector('.mock-action-btn');

  const toggleMockFocus = () => {
    isFocusActive = !isFocusActive;
    if (isFocusActive) {
      // Transition to Active Focus UI
      mockShieldIcon.textContent = '🧘';
      mockShieldState.textContent = 'active';
      mockFocusStatus.textContent = 'Deep Focus Active';
      mockFocusSubtext.textContent = 'Do Not Disturb is active. Stay stationary.';
      mockActionBtn.textContent = 'End Focus';
      shieldBtn.style.transform = 'scale(0.96)';
    } else {
      resetMockFocusState();
    }
  };

  const resetMockFocusState = () => {
    isFocusActive = false;
    mockShieldIcon.textContent = 'zzz';
    mockShieldState.textContent = 'off';
    mockFocusStatus.textContent = 'Ready when you are';
    mockFocusSubtext.textContent = 'Tap below to activate focus tracking.';
    mockActionBtn.textContent = 'Begin Focus';
    shieldBtn.style.transform = 'scale(1.0)';
  };

  if (shieldBtn) shieldBtn.addEventListener('click', toggleMockFocus);
  if (actionBtn) actionBtn.addEventListener('click', toggleMockFocus);
});
