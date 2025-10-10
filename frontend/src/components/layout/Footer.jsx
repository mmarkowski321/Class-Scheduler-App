import './Footer.css';
import Container from '../ui/Container';
import { useTranslation } from 'react-i18next';

function Footer() {
  const { t } = useTranslation('common');

  return (
    <footer className="footer">
      <Container>
        <div className="footer-content">
          <div className="footer-section">
            <div className="footer-logo">
              <h3>{t('footer.description')}</h3>
              <p>Platforma do harmonogramowania korepetycji</p>
            </div>
          </div>
          
          <div className="footer-section">
            <h4>{t('footer.forStudents')}</h4>
            <ul className="footer-links">
              <li><a href="#">{t('footer.links.findTutor')}</a></li>
              <li><a href="#">{t('footer.links.howItWorks')}</a></li>
              <li><a href="#">{t('footer.links.pricing')}</a></li>
              <li><a href="#">{t('footer.links.help')}</a></li>
            </ul>
          </div>
          
          <div className="footer-section">
            <h4>{t('footer.forTutors')}</h4>
            <ul className="footer-links">
              <li><a href="#">{t('footer.links.becomeTutor')}</a></li>
              <li><a href="#">{t('footer.links.manageSchedule')}</a></li>
              <li><a href="#">{t('footer.links.earnings')}</a></li>
              <li><a href="#">{t('footer.links.support')}</a></li>
            </ul>
          </div>
          
          <div className="footer-section">
            <h4>{t('footer.company')}</h4>
            <ul className="footer-links">
              <li><a href="#">{t('footer.links.about')}</a></li>
              <li><a href="#">{t('footer.links.contact')}</a></li>
            </ul>
          </div>
        </div>
        
        <div className="footer-bottom">
          <div className="footer-bottom-content">
            <p>{t('footer.copyright')}</p>
            <div className="footer-legal">
              <a href="#">{t('footer.legal.privacy')}</a>
              <a href="#">{t('footer.legal.terms')}</a>
              <a href="#">{t('footer.legal.cookies')}</a>
            </div>
          </div>
        </div>
      </Container>
    </footer>
  );
}

export default Footer;