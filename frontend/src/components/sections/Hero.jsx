import './Hero.css';
import Container from '../ui/Container';
import Button from '../ui/Button';
import { useTranslation } from 'react-i18next';
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

function Hero() {
  const { t } = useTranslation('common');
  const [stats, setStats] = useState({
    tutors: 0,
    monthlyLessons: 0,
    satisfiedStudents: null
  });

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await fetch('/api/stats/homepage');
        if (response.ok) {
          const data = await response.json();
          setStats({
            tutors: data.tutors || 0,
            monthlyLessons: data.monthlyLessons || 0,
            satisfiedStudents: data.satisfiedStudents ?? null
          });
        }
      } catch (error) {
        console.error('Failed to fetch stats:', error);
        // Keep default values on error
      }
    };
    fetchStats();
  }, []);

  return (
    <section className="hero">
      <Container>
        <div className="hero-content">
          <h1
            className="hero-title"
            dangerouslySetInnerHTML={{ __html: t('hero.title') }}
          />

          <p
            className="hero-description"
            dangerouslySetInnerHTML={{ __html: t('hero.description') }}
          />

          <div className="hero-actions">
            <Link to="/student" style={{ textDecoration: 'none' }}>
              <Button variant="primary" size="large" className="hero-btn">
                {t('hero.actions.startLearning')}
              </Button>
            </Link>
            <Link to="/tutor" style={{ textDecoration: 'none' }}>
              <Button variant="primary" size="large" className="hero-btn">
                {t('hero.actions.becomeTutor')}
              </Button>
            </Link>
          </div>

          <div className="hero-stats">
            <div className="stat">
              <span className="stat-number">{stats.tutors > 0 ? `${stats.tutors}+` : '0'}</span>
              <span className="stat-label">{t('hero.stats.tutors')}</span>
            </div>
            <div className="stat">
              <span className="stat-number">{stats.monthlyLessons > 0 ? `${stats.monthlyLessons}+` : '0'}</span>
              <span className="stat-label">{t('hero.stats.monthlyLessons')}</span>
            </div>
            <div className="stat">
              <span className="stat-number">
                {stats.satisfiedStudents !== null ? `${stats.satisfiedStudents}%` : '-'}
              </span>
              <span className="stat-label">{t('hero.stats.satisfiedStudents')}</span>
            </div>
          </div>
        </div>
      </Container>
    </section>
  );
}

export default Hero;
