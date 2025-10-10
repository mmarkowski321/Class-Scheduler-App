import './Container.css'

function Container({ children, className = '', size = 'large' }) {
  const containerClass = `container container-${size} ${className}`.trim()
  
  return (
    <div className={containerClass}>
      {children}
    </div>
  )
}

export default Container