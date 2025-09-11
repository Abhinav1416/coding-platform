import { SunIcon, MoonIcon } from '@heroicons/react/24/solid';

interface ThemeToggleProps {
  theme: 'light' | 'dark';
  toggleTheme: () => void;
}

const ThemeToggle = ({ theme, toggleTheme }: ThemeToggleProps) => {
  return (
    <div className="absolute top-6 right-6">
      <button
        onClick={toggleTheme}
        className={`relative inline-flex h-8 w-14 items-center rounded-full transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 ${
          theme === 'dark' ? 'bg-slate-700' : 'bg-indigo-500'
        }`}
      >
        <span
          className={`inline-block h-6 w-6 transform rounded-full bg-white transition-transform duration-300 ${
            theme === 'dark' ? 'translate-x-7' : 'translate-x-1'
          }`}
        />
        <MoonIcon className={`absolute left-2 h-4 w-4 text-white transition-opacity duration-300 ${theme === 'dark' ? 'opacity-100' : 'opacity-0'}`} />
        <SunIcon className={`absolute right-2 h-4 w-4 text-yellow-300 transition-opacity duration-300 ${theme === 'light' ? 'opacity-100' : 'opacity-0'}`} />
      </button>
    </div>
  );
};

export default ThemeToggle;