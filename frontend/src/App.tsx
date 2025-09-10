// src/App.tsx
export default function App() {
  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-100">
      <div className="p-6 max-w-sm bg-white rounded-2xl shadow-lg">
        <h1 className="text-2xl font-bold text-gray-800 mb-4">
          Hello, Tailwind + React!
        </h1>
        <p className="text-gray-600">
          This is a basic example of using Tailwind CSS with React.
        </p>
        <button className="mt-4 px-4 py-2 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700">
          Click Me
        </button>
      </div>
    </div>
  );
}
