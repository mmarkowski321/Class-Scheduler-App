import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import HomePage from './pages/main_pages/HomePage';
import LoginPage from './pages/main_pages/LoginPage';
import RegisterPage from './pages/main_pages/RegisterPage';
import VerifyEmail from './pages/main_pages/VerifyEmail';
import ForgotPasswordPage from './pages/main_pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/main_pages/ResetPasswordPage';
import AboutPage from './pages/main_pages/AboutPage';
import ContactPage from './pages/main_pages/ContactPage';
import ForStudentPage from './pages/main_pages/ForStudentPage'
import ForTutorPage from './pages/main_pages/ForTutorPage'
import StudentApp from './pages/app/StudentApp';
import TutorApp from './pages/app/TutorApp';
import AdminApp from './pages/app/AdminApp';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify" element={<VerifyEmail />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/about" element={<AboutPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/student" element={<ForStudentPage />} />
        <Route path="/tutor" element={<ForTutorPage />} />
        <Route path="/app/student/*" element={<StudentApp />} />
        <Route path="/app/tutor/*" element={<TutorApp />} />
        <Route path="/app/admin/*" element={<AdminApp />} />
      </Routes>
    </Router>
  );
}

export default App;
