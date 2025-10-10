import './Features.css';
import Container from '../ui/Container';
import { useTranslation } from 'react-i18next';

function Features() {
  const { t } = useTranslation('common');

  const features = [
    {
      key: 'smartScheduling',
      icon: '📅'
    },
    {
      key: 'tutorSearch',
      icon: '🔍'
    },
    {
      key: 'ratingSystem',
      icon: '⭐'
    },
    {
      key: 'notifications',
      icon: '🔔'
    },
    {
      key: 'communication',
      icon: '💬'
    },
    {
      key: 'progressStats',
      icon: '📊'
    }
  ];

  return (
    <section className="features" id="features">
      <Container>
        <div className="features-header">
          <h2 className="features-title">{t('features.title')}</h2>
          <p className="features-description">
            {t('features.description')}
          </p>
        </div>
        <div className="features-grid">
          {features.map((feature, index) => (
            <div key={index} className="feature-card">
              <div className="feature-icon">{feature.icon}</div>
              <h3 className="feature-title">{t(`features.list.${feature.key}.title`)}</h3>
              <p className="feature-description">{t(`features.list.${feature.key}.description`)}</p>
            </div>
          ))}
        </div>
      </Container>
    </section>
  );
}

export default Features;