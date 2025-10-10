import './Hero.css';
import Container from '../ui/Container';
import Button from '../ui/Button';
import { useTranslation } from 'react-i18next';

function Hero() {
  const { t } = useTranslation('common');

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
            <Button variant="primary" size="large" className="hero-btn">
              {t('hero.actions.startLearning')}
            </Button>
            <Button variant="primary" size="large" className="hero-btn">
              {t('hero.actions.becomeTutor')}
            </Button>
          </div>

          <div className="hero-stats">
            <div className="stat">
              <span className="stat-number">100+</span>
              <span className="stat-label">{t('hero.stats.tutors')}</span>
            </div>
            <div className="stat">
              <span className="stat-number">200+</span>
              <span className="stat-label">{t('hero.stats.monthlyLessons')}</span>
            </div>
            <div className="stat">
              <span className="stat-number">98%</span>
              <span className="stat-label">{t('hero.stats.satisfiedStudents')}</span>
            </div>
          </div>
        </div>
      </Container>
    </section>
  );
}

export default Hero;
