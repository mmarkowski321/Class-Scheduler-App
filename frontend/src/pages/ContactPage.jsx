import { useState } from 'react';
import Navbar from '../components/layout/Navbar';
import Footer from '../components/layout/Footer';
import Container from '../components/ui/Container';
import Button from '../components/ui/Button';
import { useTranslation } from 'react-i18next';
import './StaticPages.css';

export default function ContactPage() {
  const { t } = useTranslation('common');

  const [data, setData] = useState({
    name: '',
    email: '',
    topic: '',
    message: '',
  });
  const [sent, setSent] = useState(false);
  const [errors, setErrors] = useState({});

  const onChange = e => {
    const { name, value } = e.target;
    setData(d => ({ ...d, [name]: value }));
    setErrors(e => ({ ...e, [name]: undefined }));
  };

  const validate = () => {
    const e = {};
    if (!data.name.trim()) e.name = t('contact.errors.name');
    if (!data.email.trim()) e.email = t('contact.errors.email');
    else if (!/\S+@\S+\.\S+/.test(data.email)) e.email = t('contact.errors.emailInvalid');
    if (!data.message.trim()) e.message = t('contact.errors.message');
    return e;
  };

  const onSubmit = e => {
    e.preventDefault();
    const eMap = validate();
    if (Object.keys(eMap).length) return setErrors(eMap);
    // tu podepniesz backend / email
    console.log('Contact form:', data);
    setSent(true);
    setData({ name: '', email: '', topic: '', message: '' });
  };

  return (
    <div className="page-layout">
      <Navbar />
      <main className="static-main">
        <Container>
          <section className="static-hero">
            <h1>{t('contact.title')}</h1>
            <p className="lead">{t('contact.lead')}</p>
          </section>

          <section className="contact-grid">
            <form className="contact-form" onSubmit={onSubmit} noValidate>
              <div className="field">
                <label htmlFor="name">{t('contact.form.name')}</label>
                <input
                  id="name"
                  name="name"
                  value={data.name}
                  onChange={onChange}
                  placeholder={t('contact.placeholders.name')}
                  required
                />
                {errors.name && <div className="field-error">{errors.name}</div>}
              </div>

              <div className="field">
                <label htmlFor="email">{t('contact.form.email')}</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={data.email}
                  onChange={onChange}
                  placeholder="you@example.com"
                  required
                />
                {errors.email && <div className="field-error">{errors.email}</div>}
              </div>

              <div className="field">
                <label htmlFor="topic">{t('contact.form.topic')}</label>
                <input
                  id="topic"
                  name="topic"
                  value={data.topic}
                  onChange={onChange}
                  placeholder={t('contact.placeholders.topic')}
                />
              </div>

              <div className="field">
                <label htmlFor="message">{t('contact.form.message')}</label>
                <textarea
                  id="message"
                  name="message"
                  rows="5"
                  value={data.message}
                  onChange={onChange}
                  placeholder={t('contact.placeholders.message')}
                  required
                />
                {errors.message && <div className="field-error">{errors.message}</div>}
              </div>

              <Button type="submit" variant="primary" size="large" className="auth-submit">
                {t('contact.form.send')}
              </Button>

              {sent && <div className="form-success">{t('contact.success')}</div>}
            </form>

            <aside className="contact-aside">
              <div className="info-card">
                <h3>{t('contact.info.title')}</h3>
                <p>{t('contact.info.desc')}</p>
                <ul className="bullets">
                  <li>{t('contact.info.items.0')}</li>
                  <li>{t('contact.info.items.1')}</li>
                  <li>{t('contact.info.items.2')}</li>
                </ul>
              </div>
            </aside>
          </section>
        </Container>
      </main>
      <Footer />
    </div>
  );
}
