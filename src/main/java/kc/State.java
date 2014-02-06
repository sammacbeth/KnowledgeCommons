package kc;

public interface State {
	public String toString();

	public final static State NONE = new State() {
		@Override
		public String toString() {
			return "";
		}
	};
}
