import Navbar from '../../components/layout/Navbar';
import Footer from '../../components/layout/Footer';
import Container from '../../components/ui/Container';
import { useTranslation } from 'react-i18next';
import './StaticPages.css';
import Button from '../../components/ui/Button';
import { useNavigate } from 'react-router-dom';

export default function ForStudentPage() {
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  return (
    <div className="page-layout">
      <Navbar />
      <main className="static-main">
        <Container>
          <section className="static-hero">
            <h1>{t('student.title')}</h1>
            <p className="lead">{t('student.lead')}</p>
          </section>

          {/* << cards wrapper >> */}
          <section className="cards">
            <article className="card">
              <h3>{t('student.blocks.profile.title')}</h3>
              <p>{t('student.blocks.profile.desc')}</p>
              <ul className="bullets">
                <li>{t('student.blocks.profile.school')}</li>
                <li>{t('student.blocks.profile.class')}</li>
                <li>{t('student.blocks.profile.track')}</li>
                <li>{t('student.blocks.profile.goals')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('student.blocks.age.title')}</h3>
              <ul className="bullets">
                <li>{t('student.blocks.age.rule')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('student.blocks.booking.title')}</h3>
              <ul className="bullets">
                <li>{t('student.blocks.booking.email')}</li>
                <li>{t('student.blocks.booking.calendar')}</li>
                <li>{t('student.blocks.booking.reschedule')}</li>
                <li>{t('student.blocks.booking.cancel')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('student.blocks.reviews.title')}</h3>
              <ul className="bullets">
                <li>{t('student.blocks.reviews.afterLesson')}</li>
                <li>{t('student.blocks.reviews.editWindow')}</li>
                <li>{t('student.blocks.reviews.guidelines')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('student.blocks.extras.title')}</h3>
              <ul className="bullets">
                <li>{t('student.blocks.extras.favourites')}</li>
                <li>{t('student.blocks.extras.materials')}</li>
                <li>{t('student.blocks.extras.progress')}</li>
                <li>{t('student.blocks.extras.reminders')}</li>
              </ul>
            </article>

            <article className="card">
              <h3>{t('student.cta.title')}</h3>
              <p style={{ marginBottom: 16 }}>{t('student.cta.desc')}</p>
              <div style={{ display: 'flex', justifyContent: 'center' }}>
                <Button
                  variant="primary"
                  size="large"
                  onClick={() => navigate('/register')}
                >
                  {t('student.cta.button')}
                </Button>
              </div>
            </article>
          </section>
          {/* << /cards wrapper >> */}
        </Container>
      </main>
      <Footer />
    </div>
  );
}
