import { useTranslation } from 'react-i18next';
import './LanguageSwitcher.css';

function LanguageSwitcher() {
  const { i18n } = useTranslation();

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  const languages = [
    { code: 'pl', name: 'Polski', flag: '🇵🇱', displayCode: 'PL' },
    { code: 'en', name: 'English', flag: '🇺🇸', displayCode: 'US' }
  ];

  return (
    <div className="language-switcher">
      {languages.map((lang) => (
        <button
          key={lang.code}
          className={`lang-button ${i18n.language === lang.code ? 'active' : ''}`}
          onClick={() => changeLanguage(lang.code)}
          title={lang.name}
        >
          <span className="lang-text">{lang.displayCode}</span>
        </button>
      ))}
    </div>
  );
}

export default LanguageSwitcher;
