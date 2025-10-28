function Check({ label, name, checked, onChange }) {
  return (
    <label className="check">
      <input type="checkbox" name={name} checked={checked} onChange={onChange} />
      <span>{label}</span>
    </label>
  );
}

export default Check;
