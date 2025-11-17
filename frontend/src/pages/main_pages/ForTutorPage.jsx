import Navbar from '../../components/layout/Navbar';
import Footer from '../../components/layout/Footer';
import Container from '../../components/ui/Container';
import { useTranslation } from 'react-i18next';
import './StaticPages.css';
import Button from '../../components/ui/Button';
import { useNavigate } from 'react-router-dom';

export default function ForTutorPage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  return (
    <div className="page-layout">
      <Navbar />
      <main className="static-main">
        <Container>
          <section className="static-hero">
            <h1>{t('tutor.title')}</h1>
            <p className="lead">{t('tutor.lead')}</p>
          </section>

          <section className="cards">
            <article className="card">
              <h3>{t('tutor.blocks.profile.title')}</h3>
              <p>{t('tutor.blocks.profile.desc')}</p>
              <ul className="bullets">
                <li>{t('tutor.blocks.profile.education')}</li>
                <li>{t('tutor.blocks.profile.exams')}</li>
                <li>{t('tutor.blocks.profile.subjects')}</li>
                <li>{t('tutor.blocks.profile.methods')}</li>
                <li>{t('tutor.blocks.profile.photo')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('tutor.blocks.availability.title')}</h3>
              <ul className="bullets">
                <li>{t('tutor.blocks.availability.calendarSync')}</li>
                <li>{t('tutor.blocks.availability.slots')}</li>
                <li>{t('tutor.blocks.availability.buffer')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('tutor.blocks.booking.title')}</h3>
              <ul className="bullets">
                <li>{t('tutor.blocks.booking.requests')}</li>
                <li>{t('tutor.blocks.booking.confirmation')}</li>
                <li>{t('tutor.blocks.booking.link')}</li>
                <li>{t('tutor.blocks.booking.policy')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('tutor.blocks.payments.title')}</h3>
              <p>{t('tutor.blocks.payments.desc')}</p>
            </article>

            <article className="card">
              <h3>{t('tutor.blocks.extras.title')}</h3>
              <ul className="bullets">
                <li>{t('tutor.blocks.extras.materials')}</li>
                <li>{t('tutor.blocks.extras.homework')}</li>
                <li>{t('tutor.blocks.extras.reviews')}</li>
                <li>{t('tutor.blocks.extras.analytics')}</li>
              </ul>
            </article>
            <article className="card">
              <h3>{t('tutor.cta.title')}</h3>
              <p style={{ marginBottom: 16 }}>{t('tutor.cta.desc')}</p>
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <Button variant="primary" size="large" onClick={() => navigate('/register')}>
                  {t('tutor.cta.button')}
                </Button>
              </div>
            </article>
          </section>
        </Container>
      </main>
      <Footer />
    </div>
  );
}
