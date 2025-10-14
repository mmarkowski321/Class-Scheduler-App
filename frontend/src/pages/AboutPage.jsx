import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import Container from '../components/ui/Container';
import { useTranslation } from 'react-i18next';
import './StaticPages.css';

export default function AboutPage() {
  const { t } = useTranslation('common');

  return (
    <div className="page-layout">
      <Navbar />
      <main className="static-main">
        <Container>
          <section className="static-hero">
            <h1>{t('about.title')}</h1>
            <p className="lead">{t('about.lead')}</p>
          </section>

          <section className="cards">
            <article className="card">
              <h3>{t('about.blocks.mission.title')}</h3>
              <p>{t('about.blocks.mission.text')}</p>
            </article>
            <article className="card">
              <h3>{t('about.blocks.how.title')}</h3>
              <p>{t('about.blocks.how.text')}</p>
            </article>
            <article className="card">
              <h3>{t('about.blocks.values.title')}</h3>
              <ul className="bullets">
                <li>{t('about.blocks.values.items.0')}</li>
                <li>{t('about.blocks.values.items.1')}</li>
                <li>{t('about.blocks.values.items.2')}</li>
              </ul>
            </article>
          </section>
        </Container>
      </main>
      <Footer />
    </div>
  );
}
