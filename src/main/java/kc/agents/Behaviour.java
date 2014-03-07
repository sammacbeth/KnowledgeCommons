package kc.agents;

interface Behaviour {

	void initialise();

	void doBehaviour();

	void onEvent(String type, Object value);

}
