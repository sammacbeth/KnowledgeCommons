package kc;

import uk.ac.imperial.presage2.core.Action;

public class Strategy implements Action {

	final int id;
	final boolean measure;

	public Strategy(int id, boolean measure) {
		super();
		this.id = id;
		this.measure = measure;
	}

	public Strategy(int id) {
		this(id, true);
	}

	public Strategy(Strategy copy) {
		this(copy.id, copy.measure);
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Strategy))
			return false;
		Strategy other = (Strategy) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Strategy [id=" + id + ", measure=" + measure + "]";
	}

}
