import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import PrivacyPolicy from './pages/PrivacyPolicy';

// POC: no login. Dashboard is served directly at root.
function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/privacy-policy" element={<PrivacyPolicy />} />
        <Route path="/" element={<Dashboard />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
